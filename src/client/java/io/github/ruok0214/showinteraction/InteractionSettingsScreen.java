package io.github.ruok0214.showinteraction;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

public final class InteractionSettingsScreen extends Screen {
    private static final int PANEL_HEIGHT = 226;
    private final HighlightConfig config;
    private final HighlightPreferences draft;
    private final Runnable onApplied;
    private final Screen parent;
    private int left, top, panelWidth;
    private boolean saveFailed;

    public InteractionSettingsScreen(Screen parent, HighlightConfig config, Runnable onApplied) {
        super(Component.translatable("screen.show_interaction.title"));
        this.parent = parent;
        this.config = config;
        this.draft = config.copy();
        this.onApplied = onApplied;
    }

    @Override protected void init() {
        panelWidth = Math.min(420, width - 16);
        left = (width - panelWidth) / 2;
        top = Math.max(4, (height - PANEL_HEIGHT) / 2);
        int innerWidth = panelWidth - 20;
        int halfWidth = (innerWidth - 6) / 2;
        addRenderableWidget(Button.builder(globalLabel(), button -> {
            draft.enabled = !draft.enabled;
            button.setMessage(globalLabel());
        }).bounds(left + 10, top + 24, halfWidth, 20).build());
        var transparency = addRenderableWidget(new SettingSlider(left + 16 + halfWidth, top + 24, innerWidth - halfWidth - 6,
                0, 100, 1, draft.transparencyPercent,
                value -> Component.translatable("screen.show_interaction.transparency", (int) value),
                value -> draft.transparencyPercent = (int) value));
        transparency.setTooltip(Tooltip.create(Component.translatable("screen.show_interaction.transparency_hint")));

        int toggleWidth = 62;
        int toggleX = left + 108;
        int sliderX = toggleX + toggleWidth + 6;
        for (var action : HighlightHistory.Action.values()) {
            int rowY = top + 82 + action.ordinal() * 23;
            var reaction = draft.reactions.get(action);
            var toggle = addRenderableWidget(Button.builder(stateLabel(reaction.enabled), button -> {
                reaction.enabled = !reaction.enabled;
                button.setMessage(stateLabel(reaction.enabled));
            }).bounds(toggleX, rowY, toggleWidth, 20).build());
            toggle.setTooltip(Tooltip.create(actionLabel(action)));
            var duration = addRenderableWidget(new SettingSlider(sliderX, rowY, left + panelWidth - 10 - sliderX,
                    0.5, 10, 0.5, reaction.durationSeconds,
                    value -> Component.translatable("screen.show_interaction.seconds", String.format(Locale.ROOT, "%.1f", value)),
                    value -> reaction.durationSeconds = value));
            duration.setTooltip(Tooltip.create(Component.translatable(action == HighlightHistory.Action.OPEN
                    ? "screen.show_interaction.open_duration_hint" : "screen.show_interaction.duration_hint")));
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.show_interaction.save"), button -> save())
                .bounds(left + 10, top + 202, halfWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(left + 16 + halfWidth, top + 202, innerWidth - halfWidth - 6, 20).build());
    }

    private void save() {
        var previous = config.copy();
        config.copyFrom(draft);
        if (!config.save()) {
            config.copyFrom(previous);
            saveFailed = true;
            return;
        }
        onApplied.run();
        onClose();
    }

    @Override public void onClose() { minecraft.gui.setScreen(parent); }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        graphics.fill(left, top, left + panelWidth, top + PANEL_HEIGHT, 0xE820242C);
        graphics.outline(left, top, panelWidth, PANEL_HEIGHT, 0xFF677585);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, top + 7, 0xFFFFFFFF);
        graphics.centeredText(font, Component.translatable(saveFailed ? "screen.show_interaction.save_failed"
                : "screen.show_interaction.save_hint"), width / 2, top + 51, saveFailed ? 0xFFFF7777 : 0xFFB8C5D6);
        graphics.text(font, Component.translatable("screen.show_interaction.reaction"), left + 10, top + 68, 0xFFB8C5D6);
        graphics.centeredText(font, Component.translatable("screen.show_interaction.enabled"), left + 139, top + 68, 0xFFB8C5D6);
        graphics.centeredText(font, Component.translatable("screen.show_interaction.duration"),
                (left + 176 + left + panelWidth - 10) / 2, top + 68, 0xFFB8C5D6);
        for (var action : HighlightHistory.Action.values()) {
            graphics.text(font, actionLabel(action), left + 10, top + 88 + action.ordinal() * 23,
                    0xFF000000 | config.color(action));
        }
    }

    private Component globalLabel() {
        return Component.translatable("screen.show_interaction.global", stateLabel(draft.enabled));
    }
    private static Component stateLabel(boolean enabled) {
        return Component.translatable(enabled ? "options.on" : "options.off");
    }
    private static Component actionLabel(HighlightHistory.Action action) {
        return Component.translatable("screen.show_interaction.action." + action.name().toLowerCase(Locale.ROOT));
    }

    private static final class SettingSlider extends AbstractSliderButton {
        private final double minimum, maximum, step;
        private final DoubleFunction<Component> label;
        private final DoubleConsumer changed;

        SettingSlider(int x, int y, int width, double minimum, double maximum, double step, double initial,
                      DoubleFunction<Component> label, DoubleConsumer changed) {
            super(x, y, width, 20, Component.empty(), (initial - minimum) / (maximum - minimum));
            this.minimum = minimum;
            this.maximum = maximum;
            this.step = step;
            this.label = label;
            this.changed = changed;
            updateMessage();
        }
        private double settingValue() {
            return Math.clamp(minimum + Math.round(value * (maximum - minimum) / step) * step, minimum, maximum);
        }
        @Override protected void updateMessage() { setMessage(label.apply(settingValue())); }
        @Override protected void applyValue() { changed.accept(settingValue()); }
    }
}
