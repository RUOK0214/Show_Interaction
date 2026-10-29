package io.github.ruok0214.showinteraction;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class ShowInteractionClient implements ClientModInitializer {
    public static InteractionTracker tracker;
    private volatile List<InteractionTracker.Outline> outlines = List.of();

    @Override public void onInitializeClient() {
        HighlightConfig config = HighlightConfig.load();
        tracker = new InteractionTracker(config);
        var category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("show_interaction", "main"));
        KeyMapping toggle = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.show_interaction.toggle", GLFW.GLFW_KEY_H, category));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggle.consumeClick()) {
                config.enabled = !config.enabled;
                config.save();
                tracker.clear();
                if (client.player != null) client.player.sendOverlayMessage(Component.translatable(
                        config.enabled ? "message.show_interaction.enabled" : "message.show_interaction.disabled"));
            }
            tracker.tick();
            if (client.level == null) outlines = List.of();
        });
        // World reads stay on the extraction side; the renderer receives immutable geometry.
        LevelExtractionEvents.END_EXTRACTION.register(context -> outlines = tracker.extract());
        LevelRenderEvents.BEFORE_GIZMOS.register(context -> {
            try (var ignored = context.levelRenderer().collectPerFrameRenderThreadGizmos()) {
                for (var outline : outlines) {
                    for (var box : outline.boxes()) Gizmos.cuboid(box, GizmoStyle.stroke(outline.color(), outline.width()));
                }
            }
        });
    }
}
