package com.defendtheblock.client;

import com.defendtheblock.entity.turret.TurretModifier;
import com.defendtheblock.entity.turret.TurretModifiers;
import com.defendtheblock.network.TurretStatsData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

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
 *
 * <p>O botao "Fechar" e desenhado e testado a mao (nao usa
 * {@code ButtonWidget}/{@code addDrawableChild}) porque a caixa inteira muda
 * de altura conforme o texto (traduzido) precisa quebrar linha, e um widget
 * registrado uma vez em {@code init()} nao acompanharia isso.
 */
@Environment(EnvType.CLIENT)
public final class TurretStatsScreen extends Screen {

    private static final int WIDTH = 260;
    private static final int LINE_HEIGHT = 12;
    private static final int PADDING = 10;
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;

    private static TurretStatsScreen current;

    private TurretStatsData data;
    private int buttonLeft;
    private int buttonTop;

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

    /**
     * Desenha tudo a mao e <b>nao chama {@code super.render(...)}</b>.
     *
     * <p>Esse era o motivo da aba sair borrada: {@code Screen#render} do
     * vanilla comeca chamando {@code renderBackground}, que no 1.20.5+ aplica
     * um efeito de <em>blur</em> em cima de todo o framebuffer. Como a chamada
     * ao super vinha no fim deste metodo, o blur caia por cima do painel e do
     * texto que acabaram de ser desenhados — nao era falta de contraste, era
     * literalmente o desfoque do menu do vanilla aplicado sobre a aba.
     *
     * <p>Como esta tela nao registra nenhum widget (o botao "Fechar" tambem e
     * desenhado a mao, ver o javadoc da classe), pular o super nao perde nada:
     * ele so renderizaria o fundo borrado e a lista vazia de filhos. O
     * escurecimento do mundo passa a ser feito por um {@code fill} simples,
     * que da contraste sem desfocar.
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xC0000000);

        int left = width / 2 - WIDTH / 2;

        // Monta todas as linhas ANTES de desenhar qualquer coisa, ja quebradas
        // para a largura da aba — o bug antigo era desenhar direto numa caixa
        // de tamanho fixo, que nao acompanhava textos mais compridos
        // (traducoes maiores que o ingles) e deixava letras vazando pra fora.
        List<Line> lines = buildLines();

        int contentHeight = 0;
        for (Line line : lines) {
            contentHeight += line.height();
        }

        int top = Math.max(PADDING + 4, height / 2 - contentHeight / 2 - PADDING);
        int boxTop = top - PADDING;
        int boxBottom = top + contentHeight + PADDING + BUTTON_HEIGHT + PADDING;

        context.fill(left - PADDING, boxTop, left + WIDTH + PADDING, boxBottom, 0xF0100C18);
        context.fill(left - PADDING, boxTop, left + WIDTH + PADDING, boxTop + 1, 0xFF16C8D2);
        context.fill(left - PADDING, boxBottom - 1, left + WIDTH + PADDING, boxBottom, 0xFF16C8D2);
        context.fill(left - PADDING, boxTop, left - PADDING + 1, boxBottom, 0xFF16C8D2);
        context.fill(left + WIDTH + PADDING - 1, boxTop, left + WIDTH + PADDING, boxBottom, 0xFF16C8D2);

        int y = top;
        for (Line line : lines) {
            y = line.draw(context, left, y);
        }

        buttonLeft = width / 2 - BUTTON_WIDTH / 2;
        buttonTop = boxBottom - PADDING - BUTTON_HEIGHT;
        boolean hovered = mouseX >= buttonLeft && mouseX < buttonLeft + BUTTON_WIDTH
                && mouseY >= buttonTop && mouseY < buttonTop + BUTTON_HEIGHT;
        context.fill(buttonLeft, buttonTop, buttonLeft + BUTTON_WIDTH, buttonTop + BUTTON_HEIGHT,
                hovered ? 0xFF3A3252 : 0xFF241E38);
        context.drawCenteredTextWithShadow(textRenderer, ScreenTexts.DONE,
                buttonLeft + BUTTON_WIDTH / 2, buttonTop + (BUTTON_HEIGHT - 8) / 2, 0xFFE3E3EC);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= buttonLeft && mouseX < buttonLeft + BUTTON_WIDTH
                && mouseY >= buttonTop && mouseY < buttonTop + BUTTON_HEIGHT) {
            close();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ---------------------------------------------------------------- linhas

    private List<Line> buildLines() {
        List<Line> lines = new ArrayList<>();
        lines.add(new TitleLine(title));
        lines.add(new SpacerLine());
        lines.add(new TextLine(Text.translatable("turret.defendtheblock.tier." + data.tier), 0xFFE3E3EC));
        lines.add(new BarLine(data.health, data.maxHealth, 0xFF3FD86B,
                Text.translatable("screen.defendtheblock.turret_health", (int) data.health, (int) data.maxHealth)));
        lines.add(new SpacerLine());
        lines.add(new TextLine(Text.translatable("turret.defendtheblock.stats", String.format("%.1f", data.damage),
                (int) data.range, String.format("%.1f", data.reloadTicks / 20.0F)), 0xFFE3E3EC));
        lines.add(new TextLine(Text.translatable("turret.defendtheblock.ammo_line", data.ammo, data.maxAmmo),
                0xFFE3E3EC));
        lines.add(new SpacerLine());
        lines.add(new TextLine(Text.translatable("screen.defendtheblock.modules_title",
                data.modules.size(), TurretModifiers.MAX_SLOTS), 0xFFC9A6FF));
        if (data.modules.isEmpty()) {
            lines.add(new TextLine(Text.translatable("screen.defendtheblock.modules_empty"), 0xFF9A93AD));
        } else {
            for (String entry : data.modules) {
                lines.add(new TextLine(moduleLabel(entry), 0xFFE3E3EC));
            }
        }
        lines.add(new SpacerLine());
        lines.add(new TextLine(Text.translatable("screen.defendtheblock.repair_hint", itemName(data.repairItemId)),
                0xFFAFD8FF));
        lines.add(data.nextUpgradeItemId.isEmpty()
                ? new TextLine(Text.translatable("turret.defendtheblock.max_tier"), 0xFFF7D774)
                : new TextLine(Text.translatable("screen.defendtheblock.upgrade_hint",
                        itemName(data.nextUpgradeItemId), data.upgradeProgress, data.nextUpgradeCount), 0xFFAFD8FF));
        return lines;
    }

    /** Um bloco de desenho dentro da aba: pode ocupar mais de uma linha depois de quebrado. */
    private abstract class Line {
        abstract int height();

        abstract int draw(DrawContext context, int left, int y);
    }

    private final class SpacerLine extends Line {
        @Override
        int height() {
            return LINE_HEIGHT / 2;
        }

        @Override
        int draw(DrawContext context, int left, int y) {
            return y + height();
        }
    }

    private final class TitleLine extends Line {
        private final Text text;

        TitleLine(Text text) {
            this.text = text;
        }

        @Override
        int height() {
            return LINE_HEIGHT * 2;
        }

        @Override
        int draw(DrawContext context, int left, int y) {
            context.drawCenteredTextWithShadow(textRenderer, text, width / 2, y, 0xFF7EF4F7);
            return y + height();
        }
    }

    private class TextLine extends Line {
        final List<OrderedText> rows;
        final int color;

        TextLine(Text text, int color) {
            this.rows = textRenderer.wrapLines(text, WIDTH);
            this.color = color;
        }

        @Override
        int height() {
            return Math.max(1, rows.size()) * LINE_HEIGHT;
        }

        @Override
        int draw(DrawContext context, int left, int y) {
            for (OrderedText row : rows) {
                context.drawTextWithShadow(textRenderer, row, left, y, color);
                y += LINE_HEIGHT;
            }
            return y;
        }
    }

    private final class BarLine extends TextLine {
        private final float value;
        private final float max;
        private final int barColor;

        BarLine(float value, float max, int barColor, Text label) {
            super(label, 0xFFE3E3EC);
            this.value = value;
            this.max = max;
            this.barColor = barColor;
        }

        @Override
        int height() {
            return 8 + super.height();
        }

        @Override
        int draw(DrawContext context, int left, int y) {
            float ratio = max <= 0 ? 0 : Math.max(0.0F, Math.min(1.0F, value / max));
            int filled = Math.round(WIDTH * ratio);
            context.fill(left, y, left + WIDTH, y + 6, 0xFF2B2440);
            context.fill(left, y, left + filled, y + 6, barColor);
            return super.draw(context, left, y + 8);
        }
    }

    /**
     * Transforma o {@code "id:grau"} que veio pela rede em "Nome III".
     *
     * <p>Um id desconhecido (cliente com versao diferente do mod) aparece cru
     * em vez de derrubar a aba.
     */
    private static Text moduleLabel(String entry) {
        int split = entry.lastIndexOf(':');
        if (split <= 0) {
            return Text.literal(entry);
        }
        TurretModifier modifier = TurretModifier.byId(entry.substring(0, split));
        if (modifier == null) {
            return Text.literal(entry);
        }
        String raw = entry.substring(split + 1);
        String grade;
        try {
            grade = TurretModifiers.grade(Integer.parseInt(raw));
        } catch (NumberFormatException ignored) {
            grade = raw;
        }
        return Text.translatable("screen.defendtheblock.module_entry",
                Text.translatable(modifier.translationKey()), grade);
    }

    private static Text itemName(String itemId) {
        Identifier id = Identifier.tryParse(itemId);
        Item item = id == null ? null : Registries.ITEM.get(id);
        return item == null ? Text.literal(itemId) : Text.translatable(item.getTranslationKey());
    }
}
