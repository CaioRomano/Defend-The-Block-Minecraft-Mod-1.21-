package com.defendtheblock.invasion;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

/**
 * Sorteia <b>que tipo</b> de mob nasce em cada spawn da invasao.
 *
 * <p>A invasao nao tem mais uma lista fechada de mobs: ela spawna sem parar do
 * anoitecer ate o amanhecer, e cada spawn tira um tipo desta tabela. O que muda
 * noite apos noite e quais tipos ja estao liberados — o zumbi/esqueleto/creeper
 * valem desde a primeira, e os reforcos (incluindo os do Nether) vao entrando
 * conforme as invasoes passam.
 *
 * <p>Enderman fica de fora de proposito: ele nao participa das invasoes.
 */
public final class WaveComposition {

    /**
     * @param type      tipo de mob
     * @param firstWave primeira invasao em que ele pode aparecer
     * @param weight    peso no sorteio depois de liberado (maior = mais comum)
     */
    private record Rule(EntityType<? extends MobEntity> type, int firstWave, int weight) {
    }

    private static final List<Rule> RULES = List.of(
            // --- nucleo do Overworld
            new Rule(EntityType.ZOMBIE, 1, 30),
            new Rule(EntityType.SKELETON, 1, 22),
            new Rule(EntityType.CREEPER, 1, 18),
            new Rule(EntityType.SPIDER, 2, 12),
            new Rule(EntityType.HUSK, 4, 10),
            new Rule(EntityType.STRAY, 5, 8),
            new Rule(EntityType.CAVE_SPIDER, 6, 7),
            new Rule(EntityType.WITCH, 7, 4),
            new Rule(EntityType.ZOMBIE_VILLAGER, 8, 6),
            new Rule(EntityType.DROWNED, 11, 5),
            new Rule(EntityType.VINDICATOR, 14, 3),

            // --- reforcos do Nether
            new Rule(EntityType.MAGMA_CUBE, 3, 6),
            new Rule(EntityType.WITHER_SKELETON, 4, 6),
            new Rule(EntityType.BLAZE, 5, 5),
            new Rule(EntityType.ZOMBIFIED_PIGLIN, 6, 6),
            new Rule(EntityType.PIGLIN_BRUTE, 8, 3),
            new Rule(EntityType.HOGLIN, 10, 3),
            new Rule(EntityType.GHAST, 12, 2));

    private WaveComposition() {
    }

    /** Sorteia um tipo entre os liberados para a invasao informada. */
    public static EntityType<? extends MobEntity> pick(int wave, Random random) {
        int total = 0;
        for (Rule rule : RULES) {
            if (wave >= rule.firstWave()) {
                total += rule.weight();
            }
        }
        if (total <= 0) {
            return EntityType.ZOMBIE;
        }

        int roll = random.nextInt(total);
        for (Rule rule : RULES) {
            if (wave < rule.firstWave()) {
                continue;
            }
            roll -= rule.weight();
            if (roll < 0) {
                return rule.type();
            }
        }
        return EntityType.ZOMBIE;
    }

    /** Tipos ja liberados na invasao informada (usado so para diagnostico). */
    public static List<EntityType<? extends MobEntity>> unlocked(int wave) {
        List<EntityType<? extends MobEntity>> types = new ArrayList<>();
        for (Rule rule : RULES) {
            if (wave >= rule.firstWave()) {
                types.add(rule.type());
            }
        }
        return types;
    }
}
