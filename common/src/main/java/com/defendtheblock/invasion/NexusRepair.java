package com.defendtheblock.invasion;

import com.defendtheblock.config.DtbConfig;
import com.defendtheblock.registry.ModBlocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Reparo do Nexus: diamante na mao, clique no bloco.
 *
 * <p>Espelha o reparo da torreta ({@code TurretEntity#feedMaterial}) — material
 * na mao, uma unidade por clique, som de bigorna e a vida nova na action bar.
 *
 * <p><b>Por que isto e um evento e nao um {@code onUse} no bloco.</b> A
 * assinatura de {@code Block#onUse} mudou entre as duas versoes: o 1.20.1
 * recebe {@code Hand}, o 1.21 tirou o parametro e separou o caso "com item na
 * mao" em {@code onUseWithItem}. Sobrescrever o metodo exigiria uma copia do
 * {@code NexusBlock} por versao. O {@code UseBlockCallback} do Fabric tem a
 * mesma assinatura nas duas, entao o registro (em {@code DefendTheBlock}) e a
 * logica ficam um so, no codigo comum.
 */
public final class NexusRepair {

    private NexusRepair() {
    }

    /**
     * Handler do {@code UseBlockCallback}.
     *
     * <p>Devolve {@link ActionResult#PASS} em tudo que nao for um clique de
     * reparo valido: PASS deixa o jogo seguir com o comportamento normal
     * (colocar bloco, usar item), e so a interacao que de fato repara e
     * consumida.
     */
    public static ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        if (hand != Hand.MAIN_HAND || !(world instanceof ServerWorld serverWorld)) {
            return ActionResult.PASS;
        }
        BlockPos pos = hit.getBlockPos();
        if (!world.getBlockState(pos).isOf(ModBlocks.NEXUS_BLOCK)) {
            return ActionResult.PASS;
        }
        ItemStack held = player.getStackInHand(hand);
        if (!held.isOf(Items.DIAMOND)) {
            return ActionResult.PASS;
        }

        DtbConfig config = DtbConfig.get();
        if (config.nexusRepairPerDiamond <= 0) {
            return ActionResult.PASS;
        }

        InvasionData data = NexusManager.getData(serverWorld);
        // Confere a posicao, nao so o tipo de bloco: um Nexus que nao e o da
        // campanha nao existe (o segundo e rejeitado na colocacao), mas a
        // checagem deixa isto obviamente correto de graca.
        if (!data.hasNexus() || !data.getNexusPos().equals(pos) || data.isGameOver()) {
            return ActionResult.PASS;
        }

        if (data.isWaveActive() && !config.nexusRepairDuringInvasion) {
            player.sendMessage(Text.translatable("message.defendtheblock.repair_during_invasion")
                    .formatted(Formatting.RED), true);
            return ActionResult.SUCCESS;
        }

        int healed = data.healNexus(config.nexusRepairPerDiamond);
        if (healed <= 0) {
            player.sendMessage(Text.translatable("message.defendtheblock.repair_full")
                    .formatted(Formatting.GRAY), true);
            // SUCCESS mesmo sem curar: sem isso o clique cairia no
            // comportamento normal do diamante, e o jogador ficaria tentando
            // reparar sem entender por que nada acontece.
            return ActionResult.SUCCESS;
        }

        if (!player.getAbilities().creativeMode) {
            held.decrement(1);
        }
        serverWorld.playSound(null, pos, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 0.8F, 1.6F);
        serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D, 12, 0.5D, 0.4D, 0.5D, 0.02D);
        player.sendMessage(Text.translatable("message.defendtheblock.repaired",
                data.getNexusHealth(), data.getNexusMaxHealth()).formatted(Formatting.AQUA), true);
        return ActionResult.SUCCESS;
    }
}
