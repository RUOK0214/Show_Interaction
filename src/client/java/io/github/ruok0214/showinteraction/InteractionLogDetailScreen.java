package io.github.ruok0214.showinteraction;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

final class InteractionLogDetailScreen extends Screen {
    private final Screen parent;
    private final InteractionLog.Entry entry;
    private final List<FormattedCharSequence> lines = new ArrayList<>();
    private int left, top, panelWidth, panelHeight, page, pageSize, pages;

    InteractionLogDetailScreen(Screen parent, InteractionLog.Entry entry) {
        super(Component.translatable("screen.show_interaction.log_detail"));
        this.parent = parent;
        this.entry = entry;
    }

    @Override protected void init() {
        panelWidth = Math.min(440, width - 16);
        panelHeight = Math.min(320, height - 16);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        lines.clear();
        addLine("time", Component.literal(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .format(Instant.ofEpochMilli(entry.timeMillis()).atZone(ZoneId.systemDefault()))));
        addLine("action", InteractionSettingsScreen.actionLabel(entry.action()));
        addLine("block", Component.translatable(entry.displayBlock().translationKey()));
        addLine("position", Component.literal("X: " + entry.x() + "  Y: " + entry.y() + "  Z: " + entry.z()));
        addLine("world", entry.world().isBlank() ? Component.translatable("screen.show_interaction.log_unknown_world") : Component.literal(entry.world()));
        addLine("dimension", Component.translatableWithFallback("screen.show_interaction.dimension." + entry.dimension().replace(':', '.'), entry.dimension()));
        addLine("result", Component.translatable(entry.reverted() ? "screen.show_interaction.log_reverted" : "screen.show_interaction.log_observed"));
        addLine("before", Component.translatable(entry.before().translationKey()));
        addLine("after", Component.translatable(entry.after().translationKey()));
        var keys = new TreeSet<>(entry.before().properties().keySet());
        keys.addAll(entry.after().properties().keySet());
        boolean changed = false;
        for (var key : keys) {
            String before = entry.before().properties().get(key);
            String after = entry.after().properties().get(key);
            if (Objects.equals(before, after)) continue;
            changed = true;
            var property = Component.translatableWithFallback("screen.show_interaction.property." + key, key);
            addText(Component.translatable("screen.show_interaction.log_property_change", property, propertyValue(before), propertyValue(after)));
        }
        if (!changed) addText(Component.translatable("screen.show_interaction.log_no_property_change"));
        addLine("block_id", Component.literal(entry.displayBlock().id()));
        addText(Component.translatable("screen.show_interaction.log_client_note"));
        pageSize = Math.max(1, (panelHeight - 82) / 12);
        pages = Math.max(1, (lines.size() + pageSize - 1) / pageSize);
        page = Math.clamp(page, 0, pages - 1);
        var previous = addRenderableWidget(Button.builder(Component.translatable("screen.show_interaction.previous"), button -> changePage(-1))
                .bounds(left + 10, top + panelHeight - 48, 70, 20).build());
        previous.active = page > 0;
        var next = addRenderableWidget(Button.builder(Component.translatable("screen.show_interaction.next"), button -> changePage(1))
                .bounds(left + panelWidth - 80, top + panelHeight - 48, 70, 20).build());
        next.active = page + 1 < pages;
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
                .bounds(left + 10, top + panelHeight - 24, panelWidth - 20, 20).build());
    }

    private static Component propertyValue(String value) {
        return value == null ? Component.literal("—")
                : Component.translatableWithFallback("screen.show_interaction.value." + value, value);
    }

    private void addLine(String key, Component value) { addText(Component.translatable("screen.show_interaction.log_field." + key, value)); }
    private void addText(Component text) { lines.addAll(font.split(text, panelWidth - 20)); }
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
        graphics.centeredText(font, title, width / 2, top + 10, 0xFFFFFFFF);
        for (int i = 0; i < pageSize && page * pageSize + i < lines.size(); i++) {
            graphics.text(font, lines.get(page * pageSize + i), left + 10, top + 30 + i * 12, 0xFFE6EDF3);
        }
        graphics.centeredText(font, Component.translatable("screen.show_interaction.page", page + 1, pages),
                width / 2, top + panelHeight - 42, 0xFFFFFFFF);
    }
}
