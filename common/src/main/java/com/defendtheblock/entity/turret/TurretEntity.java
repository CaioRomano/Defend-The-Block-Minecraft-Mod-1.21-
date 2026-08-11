package com.defendtheblock.entity.turret;

import com.defendtheblock.compat.DtbCompat;
import com.defendtheblock.config.DtbConfig;
import com.defendtheblock.entity.ai.TurretShootGoal;
import com.defendtheblock.item.TurretModuleItem;
import com.defendtheblock.network.TurretStatsData;
import com.defendtheblock.registry.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A torreta de flechas.
 *
 * <p>E uma entidade (nao um bloco) com aparencia de besta montada num trepe: ela
 * gira para o mob mais proximo, consome flechas de verdade e aceita melhorias de
 * material e livros encantados.
 *
 * <p>Nota de implementacao: nada aqui usa {@code DataTracker} de proposito,
 * porque {@code initDataTracker} mudou de assinatura entre 1.20.1 e 1.21. O
 * estado visivel para o cliente (nivel e municao) viaja no nome customizado, que
 * o vanilla ja sincroniza sozinho.
 */
public class TurretEntity extends MobEntity {

    /** Encantamentos aceitos e o nivel maximo de cada um. */
    public static final Map<String, Integer> SUPPORTED_ENCHANTMENTS = new LinkedHashMap<>();

    static {
        SUPPORTED_ENCHANTMENTS.put("minecraft:power", 5);
        SUPPORTED_ENCHANTMENTS.put("minecraft:punch", 2);
        SUPPORTED_ENCHANTMENTS.put("minecraft:flame", 1);
        SUPPORTED_ENCHANTMENTS.put("minecraft:piercing", 4);
        SUPPORTED_ENCHANTMENTS.put("minecraft:multishot", 1);
        SUPPORTED_ENCHANTMENTS.put("minecraft:quick_charge", 3);
    }

    private int tier;
    private int ammo;
    private ItemStack ammoStack = ItemStack.EMPTY;
    /** Quantas unidades do material do proximo nivel ja foram inseridas. */
    private int upgradeProgress;

    private int power;
    private int punch;
    private int flame;
    private int piercing;
    private int multishot;
    private int quickCharge;

    /** Personalizacao por modulo, paralela e independente do nivel. */
    private final TurretModifiers modules = new TurretModifiers();

    private int cooldown;

    /**
     * Mira propria da torreta, reaplicada todo tick depois do {@code super.tick()}.
     *
     * <p><b>Por que isso existe:</b> o {@code LookControl} do vanilla roda
     * DEPOIS das goals dentro de {@code MobEntity#tickNewAi} e, como
     * {@code shouldStayHorizontal()} e true por padrao, ele forcava
     * {@code setPitch(0)} a cada tick. Ou seja: a torreta reescrevia a
     * inclinacao que a {@code TurretShootGoal} tinha acabado de calcular e
     * nunca conseguia apontar para cima nem para baixo. Como o disparo so
     * acontece quando a mira converge, ela ficava eternamente "focada" num mob
     * fora da altura exata dos olhos dela sem nunca atirar — a causa raiz de
     * "torreta parada mirando um alvo que nao consegue acertar", "nao atira em
     * quem esta abaixo" e "torreta em cima de uma torre nao acerta nada".
     *
     * <p>Guardar a mira aqui e reaplicar em {@link #tick()} resolve sem
     * depender de sobrescrever {@code LookControl}, cuja API varia entre
     * versoes.
     */
    private float aimYaw;
    private float aimPitch;

    public TurretEntity(EntityType<? extends TurretEntity> type, World world) {
        super(type, world);
        setPersistent();
        setCustomNameVisible(true);
    }

    public static DefaultAttributeContainer.Builder createTurretAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 4.0D);
    }

    /**
     * So a {@link TurretShootGoal}: ela mesma cuida de achar/trocar de alvo
     * (ver o javadoc de la para o porque de nao usar mais um
     * {@code ActiveTargetGoal} separado no target selector).
     */
    @Override
    protected void initGoals() {
        goalSelector.add(1, new TurretShootGoal(this));
    }

    /**
     * Mob quase exatamente embaixo (ou em cima) da torreta: o angulo fica
     * instavel demais para mirar de verdade. Excluir esses candidatos aqui,
     * na propria selecao de alvo, evita que a torreta "prefira" um mob assim
     * so por estar mais perto e fique inerte em vez de atirar em quem estiver
     * se aproximando dentro do campo de visao dela.
     */
    /**
     * O alvo esta dentro do <b>cone de visao</b> da torreta?
     *
     * <p>A besta e montada num trepe: ela gira 360 graus na horizontal, mas so
     * inclina ate {@code turretVerticalFovDegrees} para cima ou para baixo.
     * Isso deixa dois <b>pontos cegos</b> naturais — um cone logo acima e outro
     * logo abaixo dela — em vez do antigo remendo que so excluia quem estava
     * exatamente no eixo vertical. Um alvo fora do cone nunca e escolhido, o
     * que impede a torreta de travar mirando algo que ela nunca conseguiria
     * apontar.
     */
    public boolean isInVisionCone(LivingEntity entity) {
        double horizontal = Math.sqrt(horizontalSquaredDistanceTo(entity));
        if (horizontal < 0.35D) {
            // Praticamente em cima/embaixo: o angulo fica indefinido.
            return false;
        }
        double dy = entity.getBodyY(0.5D) - getEyeY();
        double pitch = Math.toDegrees(Math.atan2(dy, horizontal));
        return Math.abs(pitch) <= DtbConfig.get().turretVerticalFovDegrees;
    }

    /**
     * Distancia horizontal (ignora altura) ao quadrado. O alcance da torreta
     * e medido so nesse plano: senao uma torreta bem alta (no topo de uma
     * torre, por exemplo) perderia alcance efetivo contra quem se aproxima
     * pelo chao, ja que a altura consumiria parte do orcamento de uma
     * distancia 3D.
     */
    public double horizontalSquaredDistanceTo(LivingEntity entity) {
        double dx = entity.getX() - getX();
        double dz = entity.getZ() - getZ();
        return dx * dx + dz * dz;
    }

    // ------------------------------------------------------------ atributos

    public TurretTier tier() {
        return TurretTier.of(tier);
    }

    public int getTierIndex() {
        return tier;
    }

    /** Os modulos instalados. Nunca null; vazio quando a torreta e "pura". */
    public TurretModifiers modules() {
        return modules;
    }

    public double getRange() {
        return tier().range() * modules.rangeMultiplier();
    }

    public double getArrowDamage() {
        double base = tier().damage() * (1.0D + power * 0.25D) * modules.damageMultiplier();
        return base * DtbConfig.get().turretDamageMultiplier;
    }

    public int getReloadTicks() {
        // O modulo de Cadencia multiplica DEPOIS da Carga Rapida vanilla, entao
        // os dois se somam em vez de um substituir o outro.
        int reload = tier().reload() - quickCharge * 3;
        return Math.max(3, (int) Math.round(reload * modules.reloadMultiplier()));
    }

    public int getMaxAmmo() {
        return (int) Math.max(1L, Math.round(tier().maxAmmo() * modules.ammoMultiplier()));
    }

    /**
     * Quantas flechas saem por disparo. Multitiro do vanilla vale 3, e cada grau
     * do modulo de Salva soma mais uma — todas custando <b>uma</b> flecha so
     * (ver {@link #consumeAmmo()}, chamado uma unica vez por disparo).
     */
    public int getShotCount() {
        return (multishot > 0 ? 3 : 1) + modules.extraArrows();
    }

    public boolean hasAmmo() {
        return !DtbConfig.get().turretConsumesAmmo || ammo > 0;
    }

    public ItemStack getAmmoStack() {
        return ammoStack.isEmpty() ? new ItemStack(Items.ARROW) : ammoStack;
    }

    public int getPunch() {
        return punch;
    }

    public int getFlame() {
        return flame;
    }

    public int getPiercing() {
        return piercing;
    }

    public int getMultishot() {
        return multishot;
    }

    public void consumeAmmo() {
        if (DtbConfig.get().turretConsumesAmmo && ammo > 0) {
            // Catador: as vezes o disparo simplesmente nao gasta flecha.
            double save = modules.ammoSaveChance();
            if (save > 0.0D && getRandom().nextDouble() < save) {
                return;
            }
            ammo--;
            if (ammo == 0) {
                ammoStack = ItemStack.EMPTY;
            }
            updateDisplayName();
        }
    }

    public int getCooldown() {
        return cooldown;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }

    // ------------------------------------------------------------ tick / ia

    @Override
    public void tick() {
        super.tick();
        // Reaplica a mira DEPOIS do super.tick(): e la dentro que o
        // LookControl do vanilla zera o pitch (ver o javadoc de aimYaw/aimPitch).
        setYaw(aimYaw);
        bodyYaw = aimYaw;
        headYaw = aimYaw;
        setPitch(aimPitch);

        if (cooldown > 0) {
            cooldown--;
        }
        if (!getWorld().isClient && age % 40 == 0 && getCustomName() == null) {
            updateDisplayName();
        }
    }

    @Override
    public void tickMovement() {
        // A torreta e fixa: zeramos qualquer movimento residual mas deixamos a
        // gravidade agir para ela assentar no chao.
        setVelocity(0.0D, getVelocity().y, 0.0D);
        super.tickMovement();
    }

    /** Graus de tolerancia para considerar a besta "apontada" para o alvo. */
    private static final float AIM_TOLERANCE_DEGREES = 3.0F;

    /**
     * Gira suavemente a besta em direcao ao alvo.
     *
     * @return true quando a mira ja esta dentro da tolerancia (pode atirar).
     */
    public boolean aimAt(LivingEntity target) {
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double dy = target.getBodyY(0.5D) - getEyeY();
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (MathHelper.atan2(dz, dx) * 57.2957763671875D) - 90.0F;
        float targetPitch = (float) (-(MathHelper.atan2(dy, horizontal) * 57.2957763671875D));

        aimYaw = approachAngle(aimYaw, targetYaw, 20.0F);
        aimPitch = approachAngle(aimPitch, targetPitch, 20.0F);
        setYaw(aimYaw);
        bodyYaw = aimYaw;
        headYaw = aimYaw;
        setPitch(aimPitch);

        float yawError = Math.abs(MathHelper.wrapDegrees(targetYaw - aimYaw));
        float pitchError = Math.abs(MathHelper.wrapDegrees(targetPitch - aimPitch));
        return yawError <= AIM_TOLERANCE_DEGREES && pitchError <= AIM_TOLERANCE_DEGREES;
    }

    private static float approachAngle(float current, float target, float maxStep) {
        return current + MathHelper.clamp(MathHelper.wrapDegrees(target - current), -maxStep, maxStep);
    }

    // ------------------------------------------------------------ interacao

    /**
     * O clique direito de mao vazia agora abre a aba de estatisticas (em vez
     * do antigo texto no chat). Pegar a torreta de volta e feito socando ela
     * ({@link #damage}), nao mais com o clique direito.
     */
    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack held = player.getStackInHand(hand);
        if (getWorld().isClient) {
            return ActionResult.SUCCESS;
        }

        if (held.getItem() instanceof ArrowItem) {
            return loadAmmo(player, held);
        }
        if (held.isOf(Items.ENCHANTED_BOOK)) {
            return applyBook(player, held);
        }
        if (held.getItem() instanceof TurretModuleItem module) {
            return applyModule(player, held, module.modifier());
        }
        if (held.isOf(Items.GRINDSTONE)) {
            return stripModules(player);
        }
        if (!held.isEmpty()) {
            return feedMaterial(player, held);
        }

        if (player instanceof ServerPlayerEntity serverPlayer) {
            sendStats(serverPlayer, true);
        }
        return ActionResult.SUCCESS;
    }

    private ActionResult loadAmmo(PlayerEntity player, ItemStack held) {
        if (ammo >= getMaxAmmo()) {
            player.sendMessage(Text.translatable("turret.defendtheblock.full_ammo"), true);
            return ActionResult.CONSUME;
        }
        // Trocar de tipo de flecha so e permitido com o carregador vazio.
        if (ammo > 0 && getAmmoStack().getItem() != held.getItem()) {
            player.sendMessage(Text.translatable("turret.defendtheblock.full_ammo"), true);
            return ActionResult.CONSUME;
        }

        int space = getMaxAmmo() - ammo;
        int moved = Math.min(space, held.getCount());
        ammo += moved;
        ammoStack = held.copy();
        ammoStack.setCount(1);
        if (!player.getAbilities().creativeMode) {
            held.decrement(moved);
        }

        playSound(DtbCompat.CROSSBOW_LOADED, 1.0F, 1.0F);
        updateDisplayName();
        if (player instanceof ServerPlayerEntity serverPlayer) {
            sendStats(serverPlayer, false);
        }
        return ActionResult.SUCCESS;
    }

    private ActionResult applyBook(PlayerEntity player, ItemStack book) {
        Map<String, Integer> stored = DtbCompat.readEnchantments(book);
        boolean applied = false;

        for (Map.Entry<String, Integer> entry : stored.entrySet()) {
            Integer max = SUPPORTED_ENCHANTMENTS.get(entry.getKey());
            if (max == null) {
                continue;
            }
            int level = Math.min(max, entry.getValue());
            switch (entry.getKey()) {
                case "minecraft:power" -> applied |= raise(level, power, value -> power = value);
                case "minecraft:punch" -> applied |= raise(level, punch, value -> punch = value);
                case "minecraft:flame" -> applied |= raise(level, flame, value -> flame = value);
                case "minecraft:piercing" -> applied |= raise(level, piercing, value -> piercing = value);
                case "minecraft:multishot" -> applied |= raise(level, multishot, value -> multishot = value);
                case "minecraft:quick_charge" -> applied |= raise(level, quickCharge, value -> quickCharge = value);
                default -> {
                }
            }
        }

        if (!applied) {
            player.sendMessage(Text.translatable("turret.defendtheblock.bad_enchantment"), true);
            return ActionResult.CONSUME;
        }
        if (!player.getAbilities().creativeMode) {
            book.decrement(1);
        }
        playSound(SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 1.0F, 1.2F);
        player.sendMessage(Text.translatable("turret.defendtheblock.enchanted",
                Text.literal(String.join(", ", stored.keySet()))), false);
        return ActionResult.SUCCESS;
    }

    /**
     * Instala um modulo, ou sobe o grau dele se ja estiver instalado.
     *
     * <p>O livro so e consumido quando algo de fato mudou: recusar por falta de
     * slot ou por grau maximo devolve o item, senao um clique distraido custaria
     * um livro caro sem nenhum efeito.
     */
    private ActionResult applyModule(PlayerEntity player, ItemStack book, TurretModifier modifier) {
        TurretModifiers.Result result = modules.install(modifier);
        Text name = Text.translatable(modifier.translationKey());

        switch (result) {
            case NO_SLOT -> {
                player.sendMessage(Text.translatable("turret.defendtheblock.module_no_slot",
                        TurretModifiers.MAX_SLOTS), true);
                return ActionResult.CONSUME;
            }
            case MAX_GRADE -> {
                player.sendMessage(Text.translatable("turret.defendtheblock.module_max_grade", name), true);
                return ActionResult.CONSUME;
            }
            default -> {
            }
        }

        int grade = modules.grade(modifier);
        // Fortificacao mexe no atributo de vida: precisa reaplicar, e o ganho
        // entra como vida a mais em vez de cura total.
        if (modifier == TurretModifier.FORTITUDE) {
            float before = getMaxHealth();
            applyTierAttributes(false);
            heal(getMaxHealth() - before);
        } else {
            // Aljava pode ter mudado o teto de municao para baixo (config nova).
            applyTierAttributes(false);
        }

        if (!player.getAbilities().creativeMode) {
            book.decrement(1);
        }
        playSound(SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 1.0F, 1.4F);
        player.sendMessage(Text.translatable(
                result == TurretModifiers.Result.INSTALLED
                        ? "turret.defendtheblock.module_installed"
                        : "turret.defendtheblock.module_upgraded",
                name, roman(grade)), false);
        updateDisplayName();
        if (player instanceof ServerPlayerEntity serverPlayer) {
            sendStats(serverPlayer, false);
        }
        return ActionResult.SUCCESS;
    }

    /**
     * Desmonta todos os modulos com um rebolo na mao — o mesmo item que o
     * vanilla usa para tirar encantamento.
     *
     * <p>Sem isto, instalar o modulo errado seria permanente: os dois slots
     * ficariam ocupados para sempre e a unica saida seria destruir a torreta.
     * O rebolo nao e consumido, e os livros <b>nao</b> voltam — desmontar custa
     * o investimento, igual ao rebolo do vanilla.
     */
    private ActionResult stripModules(PlayerEntity player) {
        if (modules.isEmpty()) {
            player.sendMessage(Text.translatable("turret.defendtheblock.module_none"), true);
            return ActionResult.CONSUME;
        }
        modules.clear();
        // A vida maxima pode cair junto com a Fortificacao: reaplica e deixa o
        // setHealth interno cortar o excedente.
        applyTierAttributes(false);
        setHealth(Math.min(getHealth(), getMaxHealth()));
        // BLOCK_ANVIL_USE grave em vez de BLOCK_GRINDSTONE_USE: no 1.21 parte
        // dos campos de SoundEvents virou RegistryEntry<SoundEvent> e nao da
        // para saber quais sem compilar contra a versao. Este ja e usado neste
        // mesmo arquivo, entao e um som comprovadamente seguro nas duas.
        playSound(SoundEvents.BLOCK_ANVIL_USE, 0.8F, 0.6F);
        player.sendMessage(Text.translatable("turret.defendtheblock.module_stripped"), false);
        updateDisplayName();
        if (player instanceof ServerPlayerEntity serverPlayer) {
            sendStats(serverPlayer, false);
        }
        return ActionResult.SUCCESS;
    }

    /** I, II, III... para o grau aparecer como num encantamento. */
    private static Text roman(int grade) {
        return Text.literal(switch (grade) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> String.valueOf(grade);
        });
    }

    private interface LevelSetter {
        void set(int value);
    }

    private boolean raise(int level, int current, LevelSetter setter) {
        if (level <= current) {
            return false;
        }
        setter.set(level);
        return true;
    }

    /**
     * Um item na mao que nao seja flecha nem livro so faz uma de duas coisas,
     * na ordem: repara vida (se a torreta estiver ferida e o item for o
     * material de reparo do nivel atual) ou contribui para o proximo nivel
     * (se for o material de upgrade e a torreta ja estiver com vida cheia).
     * O progresso de upgrade e acumulado unidade por unidade — sobe de nivel
     * sozinha assim que atinge a quantidade necessaria.
     */
    private ActionResult feedMaterial(PlayerEntity player, ItemStack held) {
        Item repairItem = TurretTier.repairItem(tier);
        if (held.isOf(repairItem) && getHealth() < getMaxHealth()) {
            heal((float) DtbConfig.get().turretRepairHealthPerItem);
            if (!player.getAbilities().creativeMode) {
                held.decrement(1);
            }
            playSound(SoundEvents.BLOCK_ANVIL_USE, 1.0F, 1.6F);
            player.sendMessage(Text.translatable("turret.defendtheblock.repaired",
                    (int) getHealth(), (int) getMaxHealth()), true);
            if (player instanceof ServerPlayerEntity serverPlayer) {
                sendStats(serverPlayer, false);
            }
            return ActionResult.SUCCESS;
        }

        Item nextItem = TurretTier.nextUpgradeItem(tier);
        if (nextItem == null) {
            player.sendMessage(Text.translatable("turret.defendtheblock.max_tier"), true);
            return ActionResult.CONSUME;
        }
        if (!held.isOf(nextItem)) {
            player.sendMessage(Text.translatable("turret.defendtheblock.wrong_material",
                    TurretTier.nextUpgradeCount(tier), Text.translatable(nextItem.getTranslationKey())), true);
            return ActionResult.CONSUME;
        }

        upgradeProgress++;
        if (!player.getAbilities().creativeMode) {
            held.decrement(1);
        }
        int needed = TurretTier.nextUpgradeCount(tier);
        if (upgradeProgress >= needed) {
            tier++;
            upgradeProgress = 0;
            applyTierAttributes(true);
            playSound(SoundEvents.BLOCK_ANVIL_USE, 1.0F, 1.4F);
            player.sendMessage(Text.translatable("turret.defendtheblock.upgraded", tierName()), false);
            updateDisplayName();
        } else {
            // BLOCK_ANVIL_USE de novo, so que mais agudo: ITEM_ARMOR_EQUIP_IRON
            // virou RegistryEntry<SoundEvent> no 1.21 (mesmo problema do som da
            // besta carregando, ver DtbCompat.CROSSBOW_LOADED) e nao compila
            // direto nas duas versoes.
            playSound(SoundEvents.BLOCK_ANVIL_USE, 0.6F, 1.8F);
        }
        if (player instanceof ServerPlayerEntity serverPlayer) {
            sendStats(serverPlayer, false);
        }
        return ActionResult.SUCCESS;
    }

    private void pickUp(PlayerEntity player) {
        ItemStack turret = new ItemStack(ModItems.ARROW_TURRET);
        if (!player.getInventory().insertStack(turret)) {
            dropStack(turret);
        }
        if (ammo > 0) {
            ItemStack arrows = getAmmoStack().copy();
            arrows.setCount(ammo);
            if (!player.getInventory().insertStack(arrows)) {
                dropStack(arrows);
            }
        }
        playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
        discard();
    }

    /** Manda o instantaneo completo de estatisticas para o HUD/aba do cliente. */
    private void sendStats(ServerPlayerEntity player, boolean openScreen) {
        Item repairItem = TurretTier.repairItem(tier);
        Item nextItem = TurretTier.nextUpgradeItem(tier);
        List<String> installed = new ArrayList<>();
        for (Map.Entry<TurretModifier, Integer> entry : modules.installed().entrySet()) {
            installed.add(entry.getKey().id() + ":" + entry.getValue());
        }
        TurretStatsData data = new TurretStatsData(
                getId(), tier, getHealth(), getMaxHealth(), ammo, getMaxAmmo(),
                getArrowDamage(), getRange(), getReloadTicks(),
                Registries.ITEM.getId(repairItem).toString(),
                nextItem == null ? "" : Registries.ITEM.getId(nextItem).toString(),
                TurretTier.nextUpgradeCount(tier), upgradeProgress, installed, openScreen);
        DtbCompat.sendTurretStats(player, data);
    }

    public Text tierName() {
        return Text.translatable("turret.defendtheblock.tier." + tier);
    }

    private void applyTierAttributes(boolean heal) {
        float max = (float) (tier().maxHealth() * modules.healthMultiplier());
        EntityAttributeInstance maxHealth = getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(max);
        }
        if (heal) {
            setHealth(max);
        }
        ammo = Math.min(ammo, getMaxAmmo());
    }

    private void updateDisplayName() {
        setCustomName(Text.translatable("entity.defendtheblock.arrow_turret")
                .append(" ")
                .append(tierName())
                .append(Text.literal(" [" + ammo + "/" + getMaxAmmo() + "]")
                        .formatted(ammo > 0 ? Formatting.AQUA : Formatting.RED)));
    }

    // -------------------------------------------------------- comportamento

    /**
     * Um soco do jogador (qualquer ataque corpo a corpo, na verdade) nao
     * causa dano: devolve a torreta (e a municao) para as maos de quem bateu,
     * permitindo reposicionar. Dano de qualquer outra origem (mob, flecha,
     * explosao) continua funcionando normalmente.
     */
    @Override
    public boolean damage(DamageSource source, float amount) {
        if (getWorld().isClient) {
            return false;
        }
        // Sem fogo amigo: a flecha de uma torreta nunca fere outra. O dono da
        // flecha e a torreta que atirou, entao ela aparece aqui como atacante.
        if (source.getAttacker() instanceof TurretEntity) {
            return false;
        }
        if (source.getAttacker() instanceof PlayerEntity player) {
            pickUp(player);
            return false;
        }
        return super.damage(source, amount);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushAway(net.minecraft.entity.Entity entity) {
        // torreta fixa: nao empurra e nao e empurrada
    }

    @Override
    public void takeKnockback(double strength, double x, double z) {
        // imune a repulsao
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    @Override
    protected void dropInventory() {
        super.dropInventory();
        if (getWorld() instanceof ServerWorld) {
            dropStack(new ItemStack(ModItems.ARROW_TURRET));
            if (ammo > 0) {
                ItemStack arrows = getAmmoStack().copy();
                arrows.setCount(ammo);
                dropStack(arrows);
            }
        }
    }

    // ----------------------------------------------------------------- nbt

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Tier", tier);
        nbt.putInt("Ammo", ammo);
        nbt.putInt("UpgradeProgress", upgradeProgress);
        nbt.putInt("Power", power);
        nbt.putInt("Punch", punch);
        nbt.putInt("Flame", flame);
        nbt.putInt("Piercing", piercing);
        nbt.putInt("Multishot", multishot);
        nbt.putInt("QuickCharge", quickCharge);
        modules.writeNbt(nbt);
        if (!ammoStack.isEmpty()) {
            nbt.put("AmmoStack", DtbCompat.writeStack(this, ammoStack));
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        tier = MathHelper.clamp(nbt.getInt("Tier"), 0, TurretTier.MAX_TIER);
        ammo = nbt.getInt("Ammo");
        upgradeProgress = Math.max(0, nbt.getInt("UpgradeProgress"));
        power = nbt.getInt("Power");
        punch = nbt.getInt("Punch");
        flame = nbt.getInt("Flame");
        piercing = nbt.getInt("Piercing");
        multishot = nbt.getInt("Multishot");
        quickCharge = nbt.getInt("QuickCharge");
        // Antes do applyTierAttributes: a Fortificacao entra no calculo da vida
        // maxima, entao os modulos precisam ja estar carregados ali embaixo.
        modules.readNbt(nbt);
        if (nbt.contains("AmmoStack")) {
            ammoStack = DtbCompat.readStack(this, nbt.getCompound("AmmoStack"));
        }
        applyTierAttributes(false);
        updateDisplayName();
    }
}
