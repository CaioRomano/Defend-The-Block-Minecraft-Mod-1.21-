package com.defendtheblock.entity.invader;

import com.defendtheblock.config.DtbConfig;
import com.defendtheblock.entity.ai.AttackNexusGoal;
import com.defendtheblock.entity.ai.BreachObstacleGoal;
import com.defendtheblock.entity.ai.ClimbLadderGoal;
import com.defendtheblock.entity.ai.SpiderWebShotGoal;
import com.defendtheblock.entity.turret.TurretEntity;
import com.defendtheblock.mixin.MobEntityAccessor;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.random.Random;

/**
 * Sorteia as habilidades do invasor e instala as IAs correspondentes nos
 * seletores de goal do mob vanilla.
 */
public final class InvaderGoals {

    private static final double MOVE_SPEED = 1.0D;

    private InvaderGoals() {
    }

    /**
     * Sorteia as habilidades extras.
     *
     * <p>{@link InvaderAbility#LADDER_CLIMB} e dado a todos; as demais usam as
     * chances da config, sempre bem abaixo de 50% para que a maioria da horda
     * continue sendo de mobs comuns.
     */
    public static void roll(MobEntity mob, InvaderData data, Random random) {
        DtbConfig config = DtbConfig.get();
        data.addAbility(InvaderAbility.LADDER_CLIMB);

        if (mob instanceof CreeperEntity) {
            if (random.nextDouble() < config.creeperBreachChance) {
                data.addAbility(InvaderAbility.SUICIDE_BREACH);
            }
            return;
        }

        if (mob instanceof SpiderEntity) {
            // Aranhas ja sobem parede no vanilla; a teia e o unico extra.
            if (random.nextDouble() < config.spiderWebChance) {
                data.addAbility(InvaderAbility.WEB_SHOT);
            }
            return;
        }

        if (mob instanceof ZombieEntity) {
            // Uma habilidade por zumbi, no maximo: os intervalos sao exclusivos.
            double roll = random.nextDouble();
            double pickaxe = config.zombiePickaxeChance;
            double ladder = pickaxe + config.zombieLadderChance;
            double tnt = ladder + config.zombieTntChance;

            if (roll < pickaxe) {
                data.addAbility(InvaderAbility.PICKAXE_MINER);
            } else if (roll < ladder) {
                data.addAbility(InvaderAbility.LADDER_BUILDER);
            } else if (roll < tnt) {
                data.addAbility(InvaderAbility.TNT_SAPPER);
            }
        }
    }

    /**
     * Instala as goals no mob. Seguro para chamar de novo depois de recarregar o
     * chunk: as goals nao sao persistidas, os dados de invasor sim.
     */
    public static void install(MobEntity mob, InvaderData data) {
        if (data.areGoalsInstalled()) {
            return;
        }
        data.setGoalsInstalled(true);

        MobEntityAccessor accessor = (MobEntityAccessor) mob;

        // Prioridades acima (numero menor) das goals de perambular do vanilla,
        // mas abaixo das goals de ataque corpo a corpo, para o mob preferir o
        // jogador quando ele estiver por perto.
        accessor.defendtheblock$getGoalSelector().add(3, new BreachObstacleGoal(mob, MOVE_SPEED));
        accessor.defendtheblock$getGoalSelector().add(4, new ClimbLadderGoal(mob, MOVE_SPEED));
        if (data.hasAbility(InvaderAbility.WEB_SHOT)) {
            accessor.defendtheblock$getGoalSelector().add(4, new SpiderWebShotGoal(mob));
        }
        accessor.defendtheblock$getGoalSelector().add(6, new AttackNexusGoal(mob, MOVE_SPEED));

        // Mesmo indo atras do Nexus, o invasor mata quem cruzar o caminho.
        accessor.defendtheblock$getTargetSelector().add(3, new ActiveTargetGoal<>(mob, PlayerEntity.class, true));
        accessor.defendtheblock$getTargetSelector().add(4, new ActiveTargetGoal<>(mob, TurretEntity.class, true));
    }
}
