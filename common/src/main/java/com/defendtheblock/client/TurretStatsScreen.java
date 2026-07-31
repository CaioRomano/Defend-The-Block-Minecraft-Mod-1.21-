package com.defendtheblock.client;

import com.defendtheblock.network.TurretStatsData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Aba de estatisticas da torreta, aberta pelo clique direito de mao vazia
 * (ver {@code TurretEntity#interactMob}).
 *
 * <p>Nao e um {@code HandledScreen}/{@code ScreenHandler} de verdade: em vez
 * de um slot de inventario sincronizado, o material de upgrade/reparo e
 * inserido normalmente clicando com o botao direito na torreta (com a aba
 * fechada) segurando o item — o mesmo gesto usado para reparar. Reabrir a
 * aba sempre mostra o progresso mais recente. Essa escolha evita depender de
 * {@code ScreenHandlerRegistry}/{@code Slot}, familia de API sem nenhum
 * precedente testado neste projeto; ver o README para detalhes.
 */
@Environment(EnvType.CLIENT)
public final class TurretStatsScreen extends Screen {

    private static final int WIDTH = 220;
    private static final int LINE_HEIGHT = 12;

    private static TurretStatsScreen current;

    private TurretStatsData data;

    private TurretStatsScreen(TurretStatsData data) {
        super(Text.translatable("screen.defendtheblock.turret_stats"));
        this.data = data;
    }

    /** Chamado pelo receptor de rede sempre que um pacote de estatisticas chega. */
    public static void accept(TurretStatsData data) {
        if (data.openScreen) {
            MinecraftClient.getInstance().setScreen(new TurretStatsScreen(data));
        } else if (current != null && current.data.turretId == data.turretId) {
            current.data = data;
        }
    }

    @Override
    protected void init() {
        current = this;
        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> close())
                .dimensions(width / 2 - 50, height / 2 + 90, 100, 20)
                .build());
    }

    @Override
    public void removed() {
        if (current == this) {
            current = null;
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int left = width / 2 - WIDTH / 2;
        int top = height / 2 - 100;

        context.fill(left - 8, top - 10, left + WIDTH + 8, top + 210, 0xD0100C18);
        context.fill(left - 8, top - 10, left + WIDTH + 8, top - 9, 0xFF16C8D2);
        context.fill(left - 8, top + 209, left + WIDTH + 8, top + 210, 0xFF16C8D2);

        int y = top;
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, y, 0xFF7EF4F7);
        y += LINE_HEIGHT * 2;

        context.drawTextWithShadow(textRenderer, Text.translatable("turret.defendtheblock.tier." + data.tier),
                left, y, 0xFFE3E3EC);
        y += LINE_HEIGHT;

        drawBar(context, left, y, WIDTH, data.health, data.maxHealth, 0xFF3FD86B,
                Text.translatable("screen.defendtheblock.turret_health", (int) data.health, (int) data.maxHealth));
        y += LINE_HEIGHT + 6;

        context.drawTextWithShadow(textRenderer,
                Text.translatable("turret.defendtheblock.stats", String.format("%.1f", data.damage),
                        (int) data.range, String.format("%.1f", data.reloadTicks / 20.0F)),
                left, y, 0xFFE3E3EC);
        y += LINE_HEIGHT;

        context.drawTextWithShadow(textRenderer,
                Text.translatable("turret.defendtheblock.ammo_line", data.ammo, data.maxAmmo),
                left, y, 0xFFE3E3EC);
        y += LINE_HEIGHT * 2;

        context.drawTextWithShadow(textRenderer,
                Text.translatable("screen.defendtheblock.repair_hint", itemName(data.repairItemId)),
                left, y, 0xFFAFD8FF);
        y += LINE_HEIGHT;

        if (data.nextUpgradeItemId.isEmpty()) {
            context.drawTextWithShadow(textRenderer,
                    Text.translatable("turret.defendtheblock.max_tier"), left, y, 0xFFF7D774);
        } else {
            context.drawTextWithShadow(textRenderer,
                    Text.translatable("screen.defendtheblock.upgrade_hint", itemName(data.nextUpgradeItemId),
                            data.upgradeProgress, data.nextUpgradeCount),
                    left, y, 0xFFAFD8FF);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawBar(DrawContext context, int x, int y, int barWidth, float value, float max, int color,
                         Text label) {
        float ratio = max <= 0 ? 0 : Math.max(0.0F, Math.min(1.0F, value / max));
        int filled = Math.round(barWidth * ratio);
        context.fill(x, y, x + barWidth, y + 6, 0xFF2B2440);
        context.fill(x, y, x + filled, y + 6, color);
        context.drawTextWithShadow(textRenderer, label, x, y + 8, 0xFFE3E3EC);
    }

    private static Text itemName(String itemId) {
        Identifier id = Identifier.tryParse(itemId);
        Item item = id == null ? null : Registries.ITEM.get(id);
        return item == null ? Text.literal(itemId) : Text.translatable(item.getTranslationKey());
    }
}
