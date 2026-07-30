package com.defendtheblock.invasion;

import com.defendtheblock.compat.DtbCompat;
import com.defendtheblock.config.DtbConfig;
import com.defendtheblock.entity.invader.InvaderAccess;
import com.defendtheblock.entity.invader.InvaderData;
import com.defendtheblock.entity.invader.InvaderGoals;
import com.defendtheblock.network.InvasionSyncData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
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

    /** Cache da fila de spawn da onda atual (barato de reconstruir se cair). */
    private static int cachedWave = -1;
    private static double cachedMultiplier = -1.0D;
    private static List<EntityType<? extends MobEntity>> cachedOrder = List.of();

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
            } else {
                tickWave(world, data);
            }
        } else if (night && day != data.getLastWaveDay()) {
            data.setLastWaveDay(day);
            startWave(server, data, data.getWavesCompleted() + 1);
        }
    }

    // ---------------------------------------------------------------- ondas

    public static void startWave(MinecraftServer server, InvasionData data, int wave) {
        int total = WaveComposition.totalMobs(wave, data.getMultiplier());
        data.startWave(wave, total);

        Text message = Text.translatable("message.defendtheblock.wave_start", wave, total)
                .formatted(Formatting.RED);
        server.getPlayerManager().broadcast(message, false);

        ServerWorld world = server.getOverworld();
        BlockPos nexus = data.getNexusPos();
        world.playSound(null, nexus, SoundEvents.EVENT_RAID_HORN.value(), SoundCategory.HOSTILE, 4.0F, 0.9F);
    }

    public static void endWave(MinecraftServer server, InvasionData data, boolean completed) {
        int wave = data.getCurrentWave();
        despawnAllInvaders(server);
        data.finishWave(completed);

        if (completed) {
            Text message = Text.translatable("message.defendtheblock.wave_cleared", wave, data.getNexusHealth())
                    .formatted(Formatting.GREEN);
            server.getPlayerManager().broadcast(message, false);
        }
    }

    private static void tickWave(ServerWorld world, InvasionData data) {
        MinecraftServer server = world.getServer();
        if (data.getMobsSpawned() >= data.getMobsTotal() && data.getActiveInvaders().isEmpty()) {
            endWave(server, data, true);
            return;
        }

        int timer = data.getSpawnTimer();
        if (timer > 0) {
            data.setSpawnTimer(timer - 1);
            return;
        }

        DtbConfig config = DtbConfig.get();
        int wave = data.getCurrentWave();
        // A cada invasao o intervalo entre lotes encolhe e o lote cresce:
        // e o "ratespawn" aumentando noite apos noite.
        int interval = Math.max(config.minSpawnInterval, config.baseSpawnInterval - wave * 6);
        int batch = 1 + wave / 3;
        data.setSpawnTimer(interval);

        List<EntityType<? extends MobEntity>> order = spawnOrder(wave, data.getMultiplier());
        for (int i = 0; i < batch; i++) {
            if (data.getMobsSpawned() >= data.getMobsTotal()
                    || data.getMobsSpawned() >= order.size()
                    || data.getActiveInvaders().size() >= config.maxConcurrentInvaders) {
                break;
            }
            EntityType<? extends MobEntity> type = order.get(data.getMobsSpawned());
            if (!spawnInvader(world, data, type, wave)) {
                break;
            }
        }
    }

    private static List<EntityType<? extends MobEntity>> spawnOrder(int wave, double multiplier) {
        if (wave != cachedWave || multiplier != cachedMultiplier) {
            cachedWave = wave;
            cachedMultiplier = multiplier;
            cachedOrder = WaveComposition.spawnOrder(wave, multiplier);
        }
        return cachedOrder;
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

        markAsInvader(world, mob, nexus, wave, true);
        if (!world.spawnEntity(mob)) {
            return false;
        }

        data.countSpawn();
        data.addInvader(mob.getUuid());
        world.spawnParticles(ParticleTypes.SOUL, mob.getX(), mob.getY() + 0.5D, mob.getZ(), 12,
                0.4D, 0.6D, 0.4D, 0.02D);
        return true;
    }

    /** Um lugar valido no anel de spawn em volta do Nexus. */
    private static BlockPos findSpawnPos(ServerWorld world, BlockPos nexus) {
        DtbConfig config = DtbConfig.get();
        int min = Math.max(4, config.minSpawnRadius);
        int max = Math.max(min + 4, config.spawnRadius);

        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = world.getRandom().nextDouble() * Math.PI * 2.0D;
            double radius = min + world.getRandom().nextDouble() * (max - min);
            int x = nexus.getX() + (int) Math.round(Math.cos(angle) * radius);
            int z = nexus.getZ() + (int) Math.round(Math.sin(angle) * radius);

            if (!world.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }
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

    public static void markAsInvader(ServerWorld world, MobEntity mob, BlockPos nexus, int wave, boolean counts) {
        InvaderData invader = InvaderAccess.of(mob);
        if (invader == null) {
            return;
        }
        boolean fresh = !invader.isInvader();
        invader.setInvader(true);
        invader.setNexusPos(nexus);
        invader.setCountsForWave(counts);

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
        double radius = DtbConfig.get().attractionRadius;
        if (mob.getBlockPos().getSquaredDistance(data.getNexusPos()) > radius * radius) {
            return;
        }
        // Mobs atraidos nao entram na contagem oficial da onda.
        markAsInvader(world, mob, data.getNexusPos(), Math.max(1, data.getCurrentWave()), false);
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
                if (invader != null && invader.isInvader() && invader.countsForWave()) {
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

    public static void sync(MinecraftServer server, InvasionData data) {
        InvasionSyncData payload = InvasionSyncData.of(data);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            DtbCompat.sendInvasionSync(player, payload);
        }
    }

    public static void syncTo(ServerPlayerEntity player) {
        DtbCompat.sendInvasionSync(player, InvasionSyncData.of(NexusManager.getData(player.server)));
    }
}
