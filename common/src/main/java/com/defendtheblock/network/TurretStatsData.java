package com.defendtheblock.network;

import net.minecraft.network.PacketByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * Instantaneo das estatisticas de uma torreta, enviado do servidor para o
 * cliente sempre que o jogador interage com ela (abre a aba, carrega
 * municao, alimenta material). Substitui o antigo texto de chat.
 *
 * <p>A serializacao mora aqui, comum as duas versoes; so o envelope do pacote
 * (identificador + {@code PacketByteBuf} no 1.20.1, {@code CustomPayload} no
 * 1.21) muda por versao, igual a {@link InvasionSyncData}.
 */
public final class TurretStatsData {

    public final int turretId;
    public final int tier;
    public final float health;
    public final float maxHealth;
    public final int ammo;
    public final int maxAmmo;
    public final double damage;
    public final double range;
    public final int reloadTicks;
    /** ID de registro (ex: "minecraft:iron_ingot") do material de reparo do nivel atual. */
    public final String repairItemId;
    /** ID de registro do material do proximo nivel, ou "" se ja estiver no maximo. */
    public final String nextUpgradeItemId;
    public final int nextUpgradeCount;
    public final int upgradeProgress;
    /**
     * Modulos instalados, no formato {@code "id:grau"} (ex: {@code "range:2"}).
     *
     * <p>Vai como texto simples de proposito: a alternativa seria serializar o
     * enum por ordinal, e ai um cliente com uma versao diferente do mod leria o
     * modulo errado em vez de so ignorar um nome desconhecido.
     */
    public final List<String> modules;
    /** true = o cliente deve abrir a aba agora; false = so atualiza se ja estiver aberta. */
    public final boolean openScreen;

    public TurretStatsData(int turretId, int tier, float health, float maxHealth, int ammo, int maxAmmo,
                           double damage, double range, int reloadTicks, String repairItemId,
                           String nextUpgradeItemId, int nextUpgradeCount, int upgradeProgress,
                           List<String> modules, boolean openScreen) {
        this.modules = List.copyOf(modules);
        this.turretId = turretId;
        this.tier = tier;
        this.health = health;
        this.maxHealth = maxHealth;
        this.ammo = ammo;
        this.maxAmmo = maxAmmo;
        this.damage = damage;
        this.range = range;
        this.reloadTicks = reloadTicks;
        this.repairItemId = repairItemId;
        this.nextUpgradeItemId = nextUpgradeItemId;
        this.nextUpgradeCount = nextUpgradeCount;
        this.upgradeProgress = upgradeProgress;
        this.openScreen = openScreen;
    }

    public void write(PacketByteBuf buf) {
        buf.writeVarInt(turretId);
        buf.writeVarInt(tier);
        buf.writeFloat(health);
        buf.writeFloat(maxHealth);
        buf.writeVarInt(ammo);
        buf.writeVarInt(maxAmmo);
        buf.writeDouble(damage);
        buf.writeDouble(range);
        buf.writeVarInt(reloadTicks);
        buf.writeString(repairItemId);
        buf.writeString(nextUpgradeItemId);
        buf.writeVarInt(nextUpgradeCount);
        buf.writeVarInt(upgradeProgress);
        buf.writeVarInt(modules.size());
        for (String module : modules) {
            buf.writeString(module);
        }
        buf.writeBoolean(openScreen);
    }

    public static TurretStatsData read(PacketByteBuf buf) {
        int turretId = buf.readVarInt();
        int tier = buf.readVarInt();
        float health = buf.readFloat();
        float maxHealth = buf.readFloat();
        int ammo = buf.readVarInt();
        int maxAmmo = buf.readVarInt();
        double damage = buf.readDouble();
        double range = buf.readDouble();
        int reloadTicks = buf.readVarInt();
        String repairItemId = buf.readString();
        String nextUpgradeItemId = buf.readString();
        int nextUpgradeCount = buf.readVarInt();
        int upgradeProgress = buf.readVarInt();
        // Lido numa variavel, e nao inline no construtor: a ordem de avaliacao
        // dos argumentos e a ordem dos bytes no buffer, e um laco no meio da
        // lista de argumentos seria facil de reordenar por engano depois.
        int count = buf.readVarInt();
        List<String> modules = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            modules.add(buf.readString());
        }
        return new TurretStatsData(turretId, tier, health, maxHealth, ammo, maxAmmo, damage, range,
                reloadTicks, repairItemId, nextUpgradeItemId, nextUpgradeCount, upgradeProgress,
                modules, buf.readBoolean());
    }
}
