package io.github.ruok0214.showinteraction;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BooleanSupplier;

final class InteractionLogScreen extends Screen {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss");
    private final Screen parent;
    private final InteractionLog log;
    private final BooleanSupplier saveFailed;
    private List<InteractionLog.Entry> entries = List.of();
    private int left, top, panelWidth, panelHeight, page, pageSize, pages;

    InteractionLogScreen(Screen parent, InteractionLog log, BooleanSupplier saveFailed) {
        super(Component.translatable("screen.show_interaction.log_title"));
        this.parent = parent;
        this.log = log;
        this.saveFailed = saveFailed;
    }

    @Override protected void init() {
        panelWidth = Math.min(440, width - 16);
        panelHeight = Math.min(300, height - 16);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        entries = log.newestFirst();
        pageSize = Math.max(1, (panelHeight - 96) / 22);
        pages = Math.max(1, (entries.size() + pageSize - 1) / pageSize);
        page = Math.clamp(page, 0, pages - 1);
        addRenderableWidget(Button.builder(Component.translatable("screen.show_interaction.log_refresh"), button -> rebuildWidgets())
                .bounds(left + panelWidth - 74, top + 4, 64, 20).build());
        for (int i = 0; i < pageSize && page * pageSize + i < entries.size(); i++) {
            var entry = entries.get(page * pageSize + i);
            var label = Component.literal(TIME.format(Instant.ofEpochMilli(entry.timeMillis()).atZone(ZoneId.systemDefault())) + " · ")
                    .append(InteractionSettingsScreen.actionLabel(entry.action()));
            if (entry.reverted()) label.append(Component.translatable("screen.show_interaction.log_reverted_short"));
            var row = addRenderableWidget(Button.builder(label,
                    button -> minecraft.gui.setScreen(new InteractionLogDetailScreen(this, entry)))
                    .bounds(left + 10, top + 42 + i * 22, panelWidth - 20, 20).build());
            row.setTooltip(Tooltip.create(Component.translatable("screen.show_interaction.log_row_hint",
                    Component.translatable(entry.displayBlock().translationKey()))));
        }
        var previous = addRenderableWidget(Button.builder(Component.translatable("screen.show_interaction.previous"), button -> changePage(-1))
                .bounds(left + 10, top + panelHeight - 48, 70, 20).build());
        previous.active = page > 0;
        var next = addRenderableWidget(Button.builder(Component.translatable("screen.show_interaction.next"), button -> changePage(1))
                .bounds(left + panelWidth - 80, top + panelHeight - 48, 70, 20).build());
        next.active = page + 1 < pages;
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
                .bounds(left + 10, top + panelHeight - 24, panelWidth - 20, 20).build());
    }

    private void changePage(int change) {
        int target = Math.clamp(page + change, 0, pages - 1);
        if (target != page) { page = target; rebuildWidgets(); }
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (vertical != 0) { changePage(vertical > 0 ? -1 : 1); return true; }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override public void onClose() { minecraft.gui.setScreen(parent); }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xE820242C);
        graphics.outline(left, top, panelWidth, panelHeight, 0xFF677585);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.text(font, title, left + 10, top + 10, 0xFFFFFFFF);
        boolean failed = saveFailed.getAsBoolean();
        graphics.text(font, Component.translatable(failed ? "screen.show_interaction.log_save_failed"
                : "screen.show_interaction.log_summary", entries.size(), InteractionLog.LIMIT), left + 10, top + 28,
                failed ? 0xFFFF7777 : 0xFFB8C5D6);
        if (entries.isEmpty()) graphics.centeredText(font, Component.translatable("screen.show_interaction.log_empty"),
                width / 2, top + panelHeight / 2, 0xFFB8C5D6);
        graphics.centeredText(font, Component.translatable("screen.show_interaction.page", page + 1, pages),
                width / 2, top + panelHeight - 42, 0xFFFFFFFF);
    }
}
