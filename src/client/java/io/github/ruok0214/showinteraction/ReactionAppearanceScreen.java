package io.github.ruok0214.showinteraction;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Edits one reaction locally; Done returns it to the parent screen's unsaved draft. */
final class ReactionAppearanceScreen extends Screen {
    private static final int PANEL_HEIGHT = 226;
    private final Screen parent;
    private final HighlightPreferences draft;
    private final HighlightHistory.Action action;
    private final SettingSlider[] channels = new SettingSlider[3];
    private SettingSlider widthSlider;
    private EditBox hex;
    private Button done;
    private float lineWidth;
    private int color;
    private String hexText;
    private boolean updating;
    private int left, top, panelWidth;

    ReactionAppearanceScreen(Screen parent, HighlightPreferences draft, HighlightHistory.Action action) {
        super(Component.translatable("screen.show_interaction.appearance_title", InteractionSettingsScreen.actionLabel(action)));
        this.parent = parent;
        this.draft = draft;
        this.action = action;
        lineWidth = draft.lineWidth(action);
        color = draft.color(action);
        hexText = draft.reactions.get(action).color;
    }

    @Override protected void init() {
        panelWidth = Math.min(360, width - 16);
        left = (width - panelWidth) / 2;
        top = Math.max(4, (height - PANEL_HEIGHT) / 2);
        int innerWidth = panelWidth - 20;
        widthSlider = addRenderableWidget(new SettingSlider(left + 90, top + 28, panelWidth - 100,
                1, 15, 0.5, lineWidth,
                value -> Component.translatable("screen.show_interaction.line_width", String.format(Locale.ROOT, "%.1f", value)),
                value -> lineWidth = (float) value));
        widthSlider.setTooltip(Tooltip.create(Component.translatable("screen.show_interaction.line_width_hint")));
        hex = addRenderableWidget(new EditBox(font, left + 150, top + 55, panelWidth - 160, 20,
                Component.translatable("screen.show_interaction.color_hex")));
        hex.setMaxLength(7);
        hex.setValue(hexText);
        hex.setTooltip(Tooltip.create(Component.translatable("screen.show_interaction.color_hex_hint")));
        hex.setResponder(value -> {
            if (updating) return;
            hexText = value;
            if (validHex()) {
                color = Integer.parseInt(value.replace("#", ""), 16);
                syncChannels();
            }
            updateValidity();
        });
        String[] names = {"red", "green", "blue"};
        for (int i = 0; i < 3; i++) {
            int shift = 16 - i * 8;
            String name = names[i];
            channels[i] = addRenderableWidget(new SettingSlider(left + 10, top + 100 + i * 24, innerWidth,
                    0, 255, 1, (color >> shift) & 255,
                    value -> Component.translatable("screen.show_interaction." + name, (int) value),
                    value -> setColor((color & ~(255 << shift)) | ((int) value << shift))));
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.show_interaction.appearance_reset"), button -> {
            lineWidth = 3;
            widthSlider.showSetting(lineWidth);
            setColor(Integer.parseInt(HighlightPreferences.defaultColor(action).substring(1), 16));
        }).bounds(left + 10, top + 176, innerWidth, 20).build());
        int halfWidth = (innerWidth - 6) / 2;
        done = addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> {
            if (!validHex()) return;
            var reaction = draft.reactions.get(action);
            reaction.lineWidth = lineWidth;
            reaction.color = String.format(Locale.ROOT, "#%06X", color);
            onClose();
        }).bounds(left + 10, top + 202, halfWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(left + 16 + halfWidth, top + 202, innerWidth - halfWidth - 6, 20).build());
        updateValidity();
    }

    private boolean validHex() { return hexText.matches("#?[0-9a-fA-F]{6}"); }

    private void updateValidity() {
        boolean valid = validHex();
        if (done != null) done.active = valid;
        hex.setTextColor(valid ? 0xFFFFFFFF : 0xFFFF7777);
    }

    private void setColor(int newColor) {
        color = newColor;
        hexText = String.format(Locale.ROOT, "#%06X", color);
        updating = true;
        hex.setValue(hexText);
        updating = false;
        syncChannels();
        updateValidity();
    }

    private void syncChannels() {
        for (int i = 0; i < 3; i++) if (channels[i] != null) channels[i].showSetting((color >> (16 - i * 8)) & 255);
    }

    @Override public void onClose() { minecraft.gui.setScreen(parent); }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        graphics.fill(left, top, left + panelWidth, top + PANEL_HEIGHT, 0xE820242C);
        graphics.outline(left, top, panelWidth, PANEL_HEIGHT, 0xFF677585);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, top + 8, 0xFFFFFFFF);
        int x = left + 20, y = top + 30, size = 48, thickness = Math.round(lineWidth);
        int previewColor = 0xFF000000 | color;
        graphics.fill(x, y, x + size, y + size, 0xFF11151B);
        graphics.fill(x, y, x + size, y + thickness, previewColor);
        graphics.fill(x, y + size - thickness, x + size, y + size, previewColor);
        graphics.fill(x, y, x + thickness, y + size, previewColor);
        graphics.fill(x + size - thickness, y, x + size, y + size, previewColor);
        graphics.centeredText(font, Component.translatable("screen.show_interaction.preview"), left + 44, top + 84, 0xFFB8C5D6);
        graphics.text(font, Component.translatable("screen.show_interaction.color_hex"), left + 90, top + 61, 0xFFB8C5D6);
        graphics.text(font, Component.translatable(validHex() ? "screen.show_interaction.appearance_hint"
                : "screen.show_interaction.color_invalid"), left + 90, top + 82, validHex() ? 0xFFB8C5D6 : 0xFFFF7777);
    }
}
