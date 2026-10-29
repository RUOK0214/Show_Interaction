package io.github.ruok0214.showinteraction;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

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

        int toggleWidth = 40;
        int toggleX = left + 108;
        int sliderX = toggleX + toggleWidth + 6;
        int appearanceX = left + panelWidth - 112;
        int logX = left + panelWidth - 56;
        for (var action : HighlightHistory.Action.values()) {
            int rowY = top + 82 + action.ordinal() * 23;
            var reaction = draft.reactions.get(action);
            var toggle = addRenderableWidget(Button.builder(stateLabel(reaction.enabled), button -> {
                reaction.enabled = !reaction.enabled;
                button.setMessage(stateLabel(reaction.enabled));
            }).bounds(toggleX, rowY, toggleWidth, 20).build());
            toggle.setTooltip(Tooltip.create(actionLabel(action)));
            var duration = addRenderableWidget(new SettingSlider(sliderX, rowY, appearanceX - sliderX - 6,
                    0.5, 10, 0.5, reaction.durationSeconds,
                    value -> Component.translatable("screen.show_interaction.seconds", String.format(Locale.ROOT, "%.1f", value)),
                    value -> reaction.durationSeconds = value));
            duration.setTooltip(Tooltip.create(Component.translatable(action == HighlightHistory.Action.OPEN
                    ? "screen.show_interaction.open_duration_hint" : "screen.show_interaction.duration_hint")));
            var appearance = addRenderableWidget(Button.builder(Component.translatable("screen.show_interaction.appearance"),
                    button -> minecraft.gui.setScreen(new ReactionAppearanceScreen(this, draft, action)))
                    .bounds(appearanceX, rowY, 52, 20).build());
            appearance.setTooltip(Tooltip.create(Component.translatable("screen.show_interaction.appearance_title", actionLabel(action))));
            var logging = addRenderableWidget(Button.builder(stateLabel(reaction.logEnabled), button -> {
                reaction.logEnabled = !reaction.logEnabled;
                button.setMessage(stateLabel(reaction.logEnabled));
            }).bounds(logX, rowY, 46, 20).build());
            logging.setTooltip(Tooltip.create(Component.translatable("screen.show_interaction.log_toggle_hint", actionLabel(action))));
        }
        int footerWidth = (innerWidth - 12) / 3;
        addRenderableWidget(Button.builder(Component.translatable("screen.show_interaction.save"), button -> save())
                .bounds(left + 10, top + 202, footerWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.show_interaction.log_view"),
                button -> minecraft.gui.setScreen(ShowInteractionClient.createLogScreen(this)))
                .bounds(left + 16 + footerWidth, top + 202, footerWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(left + 22 + footerWidth * 2, top + 202, innerWidth - footerWidth * 2 - 12, 20).build());
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
        graphics.centeredText(font, Component.translatable("screen.show_interaction.enabled"), left + 128, top + 68, 0xFFB8C5D6);
        graphics.centeredText(font, Component.translatable("screen.show_interaction.duration"),
                (left + 154 + left + panelWidth - 118) / 2, top + 68, 0xFFB8C5D6);
        graphics.centeredText(font, Component.translatable("screen.show_interaction.log_record"), left + panelWidth - 33, top + 68, 0xFFB8C5D6);
        for (var action : HighlightHistory.Action.values()) {
            int y = top + 88 + action.ordinal() * 23;
            graphics.fill(left + 10, y, left + 17, y + 8, 0xFF000000 | draft.color(action));
            graphics.outline(left + 9, y - 1, 9, 10, 0xFFB8C5D6);
            graphics.text(font, actionLabel(action), left + 22, y, 0xFFFFFFFF);
        }
    }

    private Component globalLabel() {
        return Component.translatable("screen.show_interaction.global", stateLabel(draft.enabled));
    }
    private static Component stateLabel(boolean enabled) {
        return Component.translatable(enabled ? "options.on" : "options.off");
    }
    static Component actionLabel(HighlightHistory.Action action) {
        return Component.translatable("screen.show_interaction.action." + action.name().toLowerCase(Locale.ROOT));
    }

}
