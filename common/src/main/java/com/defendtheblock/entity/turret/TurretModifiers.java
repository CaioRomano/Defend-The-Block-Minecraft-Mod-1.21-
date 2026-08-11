package com.defendtheblock.entity.turret;

import com.defendtheblock.config.DtbConfig;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Os modulos instalados numa torreta, e toda a matematica que eles aplicam.
 *
 * <p>Fica separado de {@link TurretEntity} de proposito: a torreta ja carrega
 * nivel, municao, progresso de upgrade e seis encantamentos vanilla, e enfiar
 * mais nove campos ali dentro deixaria a classe ilegivel. Aqui o contrato e
 * pequeno — instalar, ler grau, e devolver multiplicadores prontos.
 *
 * <p>O teto de {@value #MAX_SLOTS} tipos e o que torna a escolha interessante:
 * sem ele, uma torreta de fim de jogo teria simplesmente todos os modulos e a
 * personalizacao viraria mais uma barra de progresso.
 */
public final class TurretModifiers {

    /** Quantos <b>tipos</b> diferentes de modulo cabem numa torreta. */
    public static final int MAX_SLOTS = 2;

    /** O que aconteceu numa tentativa de instalar um modulo. */
    public enum Result {
        /** Ocupou um slot livre. */
        INSTALLED,
        /** Ja estava instalado e subiu de grau. */
        UPGRADED,
        /** Ja estava no grau maximo: nada mudou, o livro nao e consumido. */
        MAX_GRADE,
        /** Os dois slots ja estao ocupados por outros tipos. */
        NO_SLOT
    }

    private final Map<TurretModifier, Integer> grades = new EnumMap<>(TurretModifier.class);

    public int grade(TurretModifier modifier) {
        return grades.getOrDefault(modifier, 0);
    }

    public boolean isEmpty() {
        return grades.isEmpty();
    }

    /** Os modulos instalados, em ordem de instalacao nao garantida. So leitura. */
    public Map<TurretModifier, Integer> installed() {
        return Collections.unmodifiableMap(grades);
    }

    public Result install(TurretModifier modifier) {
        Integer current = grades.get(modifier);
        if (current == null) {
            if (grades.size() >= MAX_SLOTS) {
                return Result.NO_SLOT;
            }
            grades.put(modifier, 1);
            return Result.INSTALLED;
        }
        if (current >= modifier.maxGrade()) {
            return Result.MAX_GRADE;
        }
        grades.put(modifier, current + 1);
        return Result.UPGRADED;
    }

    /** Remove tudo. Usado pela desmontagem com rebolo. */
    public void clear() {
        grades.clear();
    }

    // ------------------------------------------------------- multiplicadores

    public double rangeMultiplier() {
        return 1.0D + grade(TurretModifier.RANGE) * DtbConfig.get().turretModuleRangePerGrade;
    }

    public double damageMultiplier() {
        return 1.0D + grade(TurretModifier.DAMAGE) * DtbConfig.get().turretModuleDamagePerGrade;
    }

    public double healthMultiplier() {
        return 1.0D + grade(TurretModifier.FORTITUDE) * DtbConfig.get().turretModuleHealthPerGrade;
    }

    public double ammoMultiplier() {
        return 1.0D + grade(TurretModifier.QUIVER) * DtbConfig.get().turretModuleAmmoPerGrade;
    }

    /**
     * Fator do tempo de recarga (menor = atira mais rapido). O piso de 0.2
     * existe para nenhuma combinacao de config zerar o tempo de tiro e a
     * torreta virar um jato continuo de flechas.
     */
    public double reloadMultiplier() {
        double reduction = grade(TurretModifier.RAPID) * DtbConfig.get().turretModuleReloadPerGrade;
        return Math.max(0.2D, 1.0D - reduction);
    }

    /** Flechas a mais por disparo, sem custo extra de municao. */
    public int extraArrows() {
        return grade(TurretModifier.VOLLEY);
    }

    /** Chance (0..1) de o disparo nao gastar flecha. Teto de 0.9: nunca municao infinita. */
    public double ammoSaveChance() {
        return Math.min(0.9D, grade(TurretModifier.SCAVENGER) * DtbConfig.get().turretModuleSavePerGrade);
    }

    /** Duracao, em ticks, do efeito aplicado pelas flechas no grau informado. */
    public static int effectDuration(int grade) {
        return (int) Math.max(20L, Math.round(grade * DtbConfig.get().turretModuleEffectSeconds * 20.0D));
    }

    // ----------------------------------------------------------------- nbt

    public void writeNbt(NbtCompound nbt) {
        if (grades.isEmpty()) {
            return;
        }
        NbtList list = new NbtList();
        for (Map.Entry<TurretModifier, Integer> entry : grades.entrySet()) {
            NbtCompound tag = new NbtCompound();
            tag.putString("Id", entry.getKey().id());
            tag.putInt("Grade", entry.getValue());
            list.add(tag);
        }
        nbt.put("Modules", list);
    }

    public void readNbt(NbtCompound nbt) {
        grades.clear();
        NbtList list = nbt.getList("Modules", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound tag = list.getCompound(i);
            TurretModifier modifier = TurretModifier.byId(tag.getString("Id"));
            if (modifier == null) {
                // Modulo de uma versao mais nova do mod, ou renomeado: descarta
                // em vez de explodir o carregamento da entidade.
                continue;
            }
            int grade = Math.min(modifier.maxGrade(), Math.max(1, tag.getInt("Grade")));
            if (grades.size() < MAX_SLOTS) {
                grades.put(modifier, grade);
            }
        }
    }
}
