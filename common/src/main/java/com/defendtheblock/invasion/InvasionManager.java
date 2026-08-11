package com.defendtheblock.invasion;

import com.defendtheblock.compat.DtbCompat;
import com.defendtheblock.config.DtbConfig;
import com.defendtheblock.entity.invader.InvaderAccess;
import com.defendtheblock.entity.invader.InvaderData;
import com.defendtheblock.entity.invader.InvaderGoals;
import com.defendtheblock.network.InvasionSyncData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * O motor das invasoes: dispara uma onda por noite, spawna os mobs em lotes
 * cada vez mais rapidos e fecha a onda quando ela e repelida.
 */
public final class InvasionManager {

    private static final int NIGHT_START = 13000;
    private static final int NIGHT_END = 23000;
    private static final int SYNC_INTERVAL = 10;
    private static final int PRUNE_INTERVAL = 20;

    private InvasionManager() {
    }

    // ------------------------------------------------------------ tick loop

    public static void tick(MinecraftServer server) {
        NexusManager.tickShutdown(server);

        InvasionData data = NexusManager.getData(server);
        if (server.getTicks() % SYNC_INTERVAL == 0) {
            sync(server, data);
        }
        if (!data.hasNexus() || data.isGameOver()) {
            return;
        }

        ServerWorld world = server.getOverworld();
        if (server.getTicks() % PRUNE_INTERVAL == 0) {
            prune(server, data);
        }

        long timeOfDay = Math.floorMod(world.getTimeOfDay(), 24000L);
        long day = Math.floorDiv(world.getTimeOfDay(), 24000L);
        boolean night = timeOfDay >= NIGHT_START && timeOfDay < NIGHT_END;

        if (data.isWaveActive()) {
            if (!night && timeOfDay < NIGHT_START) {
                // Amanheceu: a horda que sobrou se desfaz e a noite conta como vencida.
                endWave(server, data, true);
            } else if (night) {
                tickWave(world, data);
            }
            // No fim da noite (apos NIGHT_END) a onda segue viva, mas para de
            // spawnar: quem ja nasceu termina a briga, ninguem novo aparece.
        } else {
            announceCountdown(server, data, day);
            // Carencia inicial: as primeiras noites passam em paz para o
            // jogador levantar defesa antes da estreia.
            if (night && day != data.getLastWaveDay() && !data.isInGracePeriod(day)) {
                data.setLastWaveDay(day);
                startWave(server, data, data.getWavesCompleted() + 1);
            }
        }
    }

    /**
     * Aviso na tela, uma vez por dia, enquanto a carencia inicial corre.
     *
     * <p>Nos dias anteriores mostra quantos faltam; no dia da estreia avisa que
     * a invasao e naquela noite. O dia ja anunciado fica guardado no estado
     * persistente, entao reconectar ou reiniciar o servidor nao repete o aviso.
     */
    private static void announceCountdown(MinecraftServer server, InvasionData data, long day) {
        if (data.getNexusPlacedDay() < 0L || day == data.getLastCountdownDay()) {
            return;
        }
        long remaining = data.daysUntilFirstInvasion(day);
        // Depois da estreia nao ha mais contagem para mostrar.
        if (remaining <= 0L && !data.isInGracePeriod(day) && day > data.getFirstInvasionDay()) {
            return;
        }
        data.setLastCountdownDay(day);

        if (remaining > 0L) {
            NexusManager.broadcastTitle(server,
                    Text.translatable("message.defendtheblock.grace_title").formatted(Formatting.AQUA),
                    Text.translatable("message.defendtheblock.grace_subtitle", remaining).formatted(Formatting.WHITE));
            return;
        }
        NexusManager.broadcastTitle(server,
                Text.translatable("message.defendtheblock.invasion_tonight_title").formatted(Formatting.RED),
                Text.translatable("message.defendtheblock.invasion_tonight_subtitle").formatted(Formatting.GOLD));
        server.getOverworld().playSound(null, data.getNexusPos(), SoundEvents.EVENT_RAID_HORN.value(),
                SoundCategory.HOSTILE, 3.0F, 1.2F);
    }

    // ---------------------------------------------------------------- ondas

    public static void startWave(MinecraftServer server, InvasionData data, int wave) {
        data.startWave(wave);

        Text message = Text.translatable("message.defendtheblock.wave_start", wave)
                .formatted(Formatting.RED);
        server.getPlayerManager().broadcast(message, false);

        ServerWorld world = server.getOverworld();
        BlockPos nexus = data.getNexusPos();
        world.playSound(null, nexus, SoundEvents.EVENT_RAID_HORN.value(), SoundCategory.HOSTILE, 4.0F, 0.9F);
    }

    public static void endWave(MinecraftServer server, InvasionData data, boolean completed) {
        int wave = data.getCurrentWave();
        BlockPos nexus = data.getNexusPos();
        despawnAllInvaders(server);
        // Desfaz pilares, escadas e teias que os invasores deixaram para tras.
        // Vale tanto para a noite vencida quanto para a onda cancelada: o
        // entulho nao tem por que sobreviver a invasao que o criou.
        InvaderBlocks.clearAll(server);
        data.finishWave(completed);

        if (completed) {
            Text message = Text.translatable("message.defendtheblock.wave_cleared", wave, data.getNexusHealth())
                    .formatted(Formatting.GREEN);
            server.getPlayerManager().broadcast(message, false);
            dropWaveLoot(server.getOverworld(), nexus, wave);
        }
    }

    /** Pool de recompensa espalhado ao redor do Nexus apos cada noite sobrevivida. */
    private static final List<Item> LOOT_POOL = List.of(
            Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND, Items.EMERALD, Items.COAL, Items.REDSTONE,
            Items.LAPIS_LAZULI, Items.COPPER_INGOT, Items.BREAD, Items.COOKED_BEEF, Items.APPLE,
            Items.GOLDEN_APPLE, Items.ARROW, Items.OAK_LOG, Items.COBBLESTONE, Items.STRING, Items.GUNPOWDER,
            Items.BONE, Items.LEATHER, Items.IRON_BLOCK, Items.EXPERIENCE_BOTTLE);

    /**
     * Itens valiosos aleatorios (minerios, comida, blocos...) espalhados perto
     * do Nexus depois de uma invasao repelida — a quantidade de rolagens cresce
     * (com teto) conforme as invasoes avancam.
     */
    private static void dropWaveLoot(ServerWorld world, BlockPos nexus, int wave) {
        if (nexus == null) {
            return;
        }
        Random random = world.getRandom();
        int rolls = 3 + random.nextInt(3) + Math.min(6, wave / 3);
        for (int i = 0; i < rolls; i++) {
            Item item = LOOT_POOL.get(random.nextInt(LOOT_POOL.size()));
            int count = 1 + random.nextInt(item.getMaxCount() >= 16 ? 8 : 2);
            double x = nexus.getX() + 0.5D + (random.nextDouble() - 0.5D) * 5.0D;
            double y = nexus.getY() + 1.0D;
            double z = nexus.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 5.0D;
            ItemEntity entity = new ItemEntity(world, x, y, z, new ItemStack(item, count));
            entity.setToDefaultPickupDelay();
            world.spawnEntity(entity);
        }
    }

    /**
     * A invasao nao tem mais um numero fechado de mobs: ela spawna em lotes ate
     * amanhecer, limitada apenas pelo teto de invasores vivos.
     */
    private static void tickWave(ServerWorld world, InvasionData data) {
        int timer = data.getSpawnTimer();
        if (timer > 0) {
            data.setSpawnTimer(timer - 1);
            return;
        }

        DtbConfig config = DtbConfig.get();
        int wave = data.getCurrentWave();
        // A cada invasao o intervalo entre lotes encolhe e o lote cresce:
        // e o "ratespawn" aumentando noite apos noite.
        int interval = Math.max(config.minSpawnInterval,
                config.baseSpawnInterval - wave * config.spawnIntervalStepPerWave);
        double raw = (config.baseSpawnBatch + wave * config.spawnBatchGrowthPerWave) * data.getMultiplier();
        int batch = (int) Math.max(1L, Math.round(raw));
        data.setSpawnTimer(interval);

        // O teto de invasores vivos tambem sobe por invasao, senao a escalada
        // empaca assim que a onda passa a viver encostada no limite.
        int cap = Math.min(config.maxConcurrentInvaders,
                config.baseConcurrentInvaders + wave * config.concurrentInvadersPerWave);

        for (int i = 0; i < batch; i++) {
            if (data.getActiveInvaders().size() >= cap) {
                break;
            }
            EntityType<? extends MobEntity> type = WaveComposition.pick(wave, world.getRandom());
            if (!spawnInvader(world, data, type, wave)) {
                break;
            }
            // O zumbi e a base da horda: ele chega em grupo, os outros tipos
            // chegam um a um. Isso e diferente de so aumentar o peso no
            // sorteio (que mudaria a proporcao) — aqui muda a quantidade.
            if (type == EntityType.ZOMBIE) {
                for (int extra = 0; extra < zombieExtras(wave); extra++) {
                    if (data.getActiveInvaders().size() >= cap
                            || !spawnInvader(world, data, EntityType.ZOMBIE, wave)) {
                        break;
                    }
                }
            }
        }
    }

    /**
     * Quantos zumbis extras acompanham cada zumbi sorteado nesta invasao.
     *
     * <p>Liberado aos poucos: nas primeiras noites o zumbi chega sozinho, e o
     * grupo so vai engrossando conforme as invasoes passam. Sem isso a onda 1
     * ja vinha com o triplo de zumbis por lote.
     */
    private static int zombieExtras(int wave) {
        DtbConfig config = DtbConfig.get();
        int perStep = Math.max(1, config.zombieExtraSpawnWavesPerStep);
        return Math.min(config.zombieExtraSpawnCount, Math.max(0, wave - 1) / perStep);
    }

    // ---------------------------------------------------------------- spawn

    private static boolean spawnInvader(ServerWorld world, InvasionData data,
                                        EntityType<? extends MobEntity> type, int wave) {
        BlockPos nexus = data.getNexusPos();
        BlockPos pos = findSpawnPos(world, nexus);
        if (pos == null) {
            return false;
        }

        MobEntity mob = type.create(world);
        if (mob == null) {
            return false;
        }
        mob.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                world.getRandom().nextFloat() * 360.0F, 0.0F);
        DtbCompat.initializeMob(world, mob, pos);

        markAsInvader(world, mob, nexus, wave);
        if (!world.spawnEntity(mob)) {
            return false;
        }

        data.countSpawn();
        data.addInvader(mob.getUuid());
        world.spawnParticles(ParticleTypes.SOUL, mob.getX(), mob.getY() + 0.5D, mob.getZ(), 12,
                0.4D, 0.6D, 0.4D, 0.02D);
        return true;
    }

    /**
     * Um lugar valido dentro do <b>anel de invasao</b>: fora do quadrado livre
     * de spawn em volta do Nexus e dentro do circulo externo. A geometria toda
     * mora em {@link NexusZones}, compartilhada com a checagem que impede o
     * spawn natural perto do bloco.
     *
     * <p>Fora desse anel o mundo segue com o spawn normal do vanilla.
     */
    private static BlockPos findSpawnPos(ServerWorld world, BlockPos nexus) {
        int outer = NexusZones.outerRadius();
        ChunkPos origin = new ChunkPos(nexus);

        for (int attempt = 0; attempt < 32; attempt++) {
            int dx = world.getRandom().nextInt(outer * 2 + 1) - outer;
            int dz = world.getRandom().nextInt(outer * 2 + 1) - outer;

            ChunkPos chunk = new ChunkPos(origin.x + dx, origin.z + dz);
            if (!NexusZones.isInvasionSpawnZone(nexus, chunk)) {
                continue;
            }
            if (!world.getChunkManager().isChunkLoaded(chunk.x, chunk.z)) {
                continue;
            }
            int x = chunk.getStartX() + world.getRandom().nextInt(16);
            int z = chunk.getStartZ() + world.getRandom().nextInt(16);
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y <= world.getBottomY() + 1) {
                continue;
            }
            BlockPos pos = new BlockPos(x, y, z);
            boolean feetFree = world.getBlockState(pos).isAir();
            boolean headFree = world.getBlockState(pos.up()).isAir();
            boolean ground = world.getBlockState(pos.down()).isSolidBlock(world, pos.down());
            if (feetFree && headFree && ground) {
                return pos;
            }
        }
        return null;
    }

    // ------------------------------------------------------------ invasores

    /**
     * Transforma um mob em invasor. Quem entra na <b>contagem oficial</b> da
     * onda e decidido em outro lugar: so o lote spawnado por
     * {@link #spawnInvader} chama {@code data.addInvader(uuid)}, e e esse
     * conjunto que o HUD le. Mob atraido ou invocado a mao vira invasor sem
     * entrar na conta.
     */
    public static void markAsInvader(ServerWorld world, MobEntity mob, BlockPos nexus, int wave) {
        InvaderData invader = InvaderAccess.of(mob);
        if (invader == null) {
            return;
        }
        boolean fresh = !invader.isInvader();
        invader.setInvader(true);
        invader.setNexusPos(nexus);

        if (fresh) {
            invader.setWave(wave);
            InvaderGoals.roll(mob, invader, world.getRandom());
            InvaderEquipment.equip(world, mob, invader, wave, world.getRandom());
            mob.setPersistent();
        }
        InvaderGoals.install(mob, invader);
    }

    /**
     * Chamado no carregamento de qualquer entidade. Faz duas coisas: reinstala as
     * goals de um invasor que voltou do disco, e atrai para o Nexus qualquer mob
     * hostil que nasca dentro do raio de atracao.
     */
    public static void onEntityLoad(Entity entity, ServerWorld world) {
        if (!(entity instanceof MobEntity mob)) {
            return;
        }
        InvaderData invader = InvaderAccess.of(mob);
        if (invader == null) {
            return;
        }
        if (invader.isInvader()) {
            InvaderGoals.install(mob, invader);
            return;
        }
        if (!(mob instanceof Monster) || !world.getRegistryKey().equals(World.OVERWORLD)) {
            return;
        }

        InvasionData data = NexusManager.getData(world);
        if (!data.hasNexus() || data.isGameOver()) {
            return;
        }

        ChunkPos mobChunk = new ChunkPos(mob.getBlockPos());

        // Zona livre de spawn: o quintal em volta do Nexus fica limpo. Vale
        // ate para o enderman, que e excecao so na hora de ser recrutado.
        // Nao e um escudo que apaga mob por perto: isto so roda quando a
        // entidade entra no mundo, entao quem VEIO andando de fora continua
        // valendo — some so o que tentar nascer aqui dentro.
        if (NexusZones.isNoSpawnZone(data.getNexusPos(), mobChunk)) {
            entity.discard();
            return;
        }

        // Enderman e a unica excecao: ele nunca e recrutado pela invasao.
        if (mob instanceof EndermanEntity) {
            return;
        }
        if (NexusZones.chunkDistance(new ChunkPos(data.getNexusPos()), mobChunk)
                > DtbConfig.get().attractionChunkRadius) {
            return;
        }
        // Mobs atraidos viram invasores, mas nao entram na contagem oficial da
        // onda: quem conta e o conjunto activeInvaders, alimentado so pelo lote
        // de spawnInvader.
        markAsInvader(world, mob, data.getNexusPos(), Math.max(1, data.getCurrentWave()));
    }

    private static void prune(MinecraftServer server, InvasionData data) {
        Iterator<UUID> iterator = data.getActiveInvaders().iterator();
        boolean changed = false;
        while (iterator.hasNext()) {
            Entity entity = findEntity(server, iterator.next());
            if (entity == null || !entity.isAlive()) {
                iterator.remove();
                changed = true;
            }
        }
        if (changed) {
            data.markDirty();
        }
    }

    private static Entity findEntity(MinecraftServer server, UUID uuid) {
        for (ServerWorld world : server.getWorlds()) {
            Entity entity = world.getEntity(uuid);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    public static void despawnAllInvaders(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            List<Entity> doomed = new ArrayList<>();
            for (Entity entity : world.iterateEntities()) {
                InvaderData invader = InvaderAccess.of(entity);
                // Todo invasor precisa sumir aqui, contado na onda ou nao: sem
                // isso um mob atraido (recrutado fora do lote oficial)
                // sobrevive para sempre a trocas de Nexus e continua "atacando
                // o vento" no lugar onde o Nexus costumava estar.
                if (invader != null && invader.isInvader()) {
                    doomed.add(entity);
                }
            }
            for (Entity entity : doomed) {
                world.spawnParticles(ParticleTypes.SOUL, entity.getX(), entity.getY() + 0.5D, entity.getZ(),
                        8, 0.3D, 0.5D, 0.3D, 0.02D);
                entity.discard();
            }
        }
    }

    // ----------------------------------------------------------------- sync

    /** Dia do mundo, contado sempre pelo Overworld (onde o Nexus vive). */
    private static long currentDay(MinecraftServer server) {
        return Math.floorDiv(server.getOverworld().getTimeOfDay(), 24000L);
    }

    public static void sync(MinecraftServer server, InvasionData data) {
        InvasionSyncData payload = InvasionSyncData.of(data, currentDay(server));
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            DtbCompat.sendInvasionSync(player, payload);
        }
    }

    public static void syncTo(ServerPlayerEntity player) {
        DtbCompat.sendInvasionSync(player,
                InvasionSyncData.of(NexusManager.getData(player.server), currentDay(player.server)));
    }
}
