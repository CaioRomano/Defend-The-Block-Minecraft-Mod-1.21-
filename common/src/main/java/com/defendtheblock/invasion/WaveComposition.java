package com.defendtheblock.invasion;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Decide quem aparece em cada invasao.
 *
 * <p>A invasao 1 e exatamente <b>2 creepers, 3 zumbis e 2 esqueletos</b>. A
 * partir dai cada regra cresce linearmente e novos tipos vao entrando, incluindo
 * mobs do Nether. O multiplicador global (config ou {@code /dtb multiplier})
 * escala tudo no final.
 */
public final class WaveComposition {

    /**
     * @param type      tipo de mob
     * @param firstWave primeira invasao em que ele aparece
     * @param base      quantidade na {@code firstWave}
     * @param growth    quanto cresce por invasao a partir dai
     */
    private record Rule(EntityType<? extends MobEntity> type, int firstWave, int base, double growth) {
    }

    private static final List<Rule> RULES = List.of(
            // --- nucleo do Overworld
            new Rule(EntityType.ZOMBIE, 1, 3, 1.2D),
            new Rule(EntityType.SKELETON, 1, 2, 0.9D),
            new Rule(EntityType.CREEPER, 1, 2, 0.6D),
            new Rule(EntityType.SPIDER, 2, 1, 0.5D),
            new Rule(EntityType.HUSK, 4, 1, 0.4D),
            new Rule(EntityType.STRAY, 5, 1, 0.4D),
            new Rule(EntityType.CAVE_SPIDER, 6, 1, 0.4D),
            new Rule(EntityType.WITCH, 7, 1, 0.25D),
            new Rule(EntityType.ZOMBIE_VILLAGER, 8, 1, 0.3D),
            new Rule(EntityType.ENDERMAN, 9, 1, 0.2D),
            new Rule(EntityType.DROWNED, 11, 1, 0.3D),
            new Rule(EntityType.VINDICATOR, 14, 1, 0.25D),

            // --- reforcos do Nether
            new Rule(EntityType.MAGMA_CUBE, 3, 1, 0.4D),
            new Rule(EntityType.WITHER_SKELETON, 4, 1, 0.33D),
            new Rule(EntityType.BLAZE, 5, 1, 0.3D),
            new Rule(EntityType.ZOMBIFIED_PIGLIN, 6, 1, 0.35D),
            new Rule(EntityType.PIGLIN_BRUTE, 8, 1, 0.25D),
            new Rule(EntityType.HOGLIN, 10, 1, 0.25D),
            new Rule(EntityType.GHAST, 12, 1, 0.15D));

    private WaveComposition() {
    }

    /** Quantidade de cada tipo na invasao pedida, ja com o multiplicador aplicado. */
    public static List<CountedType> counts(int wave, double multiplier) {
        List<CountedType> result = new ArrayList<>();
        for (Rule rule : RULES) {
            if (wave < rule.firstWave()) {
                continue;
            }
            int base = rule.base() + (int) Math.floor((wave - rule.firstWave()) * rule.growth());
            if (base <= 0) {
                continue;
            }
            int scaled = (int) Math.max(1L, Math.round(base * multiplier));
            result.add(new CountedType(rule.type(), scaled));
        }
        return result;
    }

    public static int totalMobs(int wave, double multiplier) {
        int total = 0;
        for (CountedType entry : counts(wave, multiplier)) {
            total += entry.count();
        }
        return total;
    }

    /**
     * Ordem de spawn da invasao: os tipos sao intercalados (round-robin) para a
     * onda chegar misturada em vez de vir um bloco de cada mob.
     *
     * <p>E deterministico de proposito: se o servidor reiniciar no meio de uma
     * invasao da para reconstruir a fila e continuar de {@code mobsSpawned}.
     */
    public static List<EntityType<? extends MobEntity>> spawnOrder(int wave, double multiplier) {
        List<CountedType> counts = counts(wave, multiplier);
        List<EntityType<? extends MobEntity>> order = new ArrayList<>();
        int[] left = new int[counts.size()];
        int remaining = 0;
        for (int i = 0; i < counts.size(); i++) {
            left[i] = counts.get(i).count();
            remaining += left[i];
        }
        while (remaining > 0) {
            for (int i = 0; i < counts.size(); i++) {
                if (left[i] > 0) {
                    order.add(counts.get(i).type());
                    left[i]--;
                    remaining--;
                }
            }
        }
        return order;
    }

    public record CountedType(EntityType<? extends MobEntity> type, int count) {
    }
}
