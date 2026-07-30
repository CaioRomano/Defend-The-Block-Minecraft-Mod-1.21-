package com.defendtheblock.client;

import com.defendtheblock.network.InvasionSyncData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Painel lateral com o andamento da campanha: invasoes ja sobrevividas, invasao
 * atual, quantos mobs ainda faltam, o multiplicador e a vida do Nexus.
 */
@Environment(EnvType.CLIENT)
public final class InvasionHud {

    private static final int PANEL_WIDTH = 148;
    private static final int MARGIN = 6;
    private static final int LINE_HEIGHT = 11;

    private static final int BACKGROUND = 0xA0100C18;
    private static final int BORDER = 0xFF16C8D2;
    private static final int TITLE_COLOR = 0xFF7EF4F7;
    private static final int TEXT_COLOR = 0xFFE3E3EC;
    private static final int BAR_BACKGROUND = 0xFF2B2440;

    private InvasionHud() {
    }

    public static void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) {
            return;
        }
        InvasionSyncData data = ClientInvasionState.get();
        if (!data.hasNexus) {
            return;
        }

        TextRenderer font = client.textRenderer;
        int x = client.getWindow().getScaledWidth() - PANEL_WIDTH - MARGIN;
        int y = MARGIN;
        int lines = 5;
        int height = 8 + LINE_HEIGHT * lines + 12;

        context.fill(x, y, x + PANEL_WIDTH, y + height, BACKGROUND);
        context.fill(x, y, x + PANEL_WIDTH, y + 1, BORDER);
        context.fill(x, y + height - 1, x + PANEL_WIDTH, y + height, BORDER);
        context.fill(x, y, x + 1, y + height, BORDER);
        context.fill(x + PANEL_WIDTH - 1, y, x + PANEL_WIDTH, y + height, BORDER);

        int textX = x + 6;
        int textY = y + 6;

        Text title = data.gameOver
                ? Text.translatable("message.defendtheblock.nexus_destroyed").formatted(Formatting.DARK_RED)
                : Text.translatable("hud.defendtheblock.title");
        context.drawTextWithShadow(font, title, textX, textY, TITLE_COLOR);
        textY += LINE_HEIGHT + 2;

        context.drawTextWithShadow(font,
                Text.translatable("hud.defendtheblock.waves_completed", data.wavesCompleted),
                textX, textY, TEXT_COLOR);
        textY += LINE_HEIGHT;

        if (data.waveActive) {
            context.drawTextWithShadow(font,
                    Text.translatable("hud.defendtheblock.current_wave", data.currentWave),
                    textX, textY, TEXT_COLOR);
            textY += LINE_HEIGHT;
            context.drawTextWithShadow(font,
                    Text.translatable("hud.defendtheblock.mobs", data.mobsRemaining, data.mobsTotal),
                    textX, textY, 0xFFFF7A6B);
        } else {
            Text idle = data.gameOver
                    ? Text.translatable("hud.defendtheblock.idle")
                    : Text.translatable("hud.defendtheblock.next_wave");
            context.drawTextWithShadow(font, idle, textX, textY, TEXT_COLOR);
            textY += LINE_HEIGHT;
            context.drawTextWithShadow(font,
                    Text.translatable("hud.defendtheblock.multiplier", String.format("%.2f", data.multiplier)),
                    textX, textY, TEXT_COLOR);
        }
        textY += LINE_HEIGHT;

        drawNexusBar(context, font, data, textX, textY, PANEL_WIDTH - 12);
    }

    private static void drawNexusBar(DrawContext context, TextRenderer font, InvasionSyncData data,
                                     int x, int y, int width) {
        int max = Math.max(1, data.nexusMaxHealth);
        float ratio = Math.max(0.0F, Math.min(1.0F, data.nexusHealth / (float) max));
        int filled = Math.round(width * ratio);

        context.fill(x, y, x + width, y + 6, BAR_BACKGROUND);
        context.fill(x, y, x + filled, y + 6, healthColor(ratio));

        Text label = Text.translatable("hud.defendtheblock.nexus_health", data.nexusHealth, max);
        context.drawTextWithShadow(font, label, x, y + 8, TEXT_COLOR);
    }

    private static int healthColor(float ratio) {
        if (ratio > 0.6F) {
            return 0xFF3FD86B;
        }
        return ratio > 0.3F ? 0xFFE7C13B : 0xFFD8402F;
    }
}
