package io.github.ruok0214.showinteraction;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

public final class ShowInteractionClient implements ClientModInitializer {
    public static InteractionTracker tracker;
    private static ShowInteractionClient instance;
    private HighlightConfig config;
    private volatile List<InteractionTracker.Outline> outlines = List.of();
    private boolean settingsRequested;

    @Override public void onInitializeClient() {
        instance = this;
        config = HighlightConfig.load();
        tracker = new InteractionTracker(config);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommands.literal("showinteraction").executes(context -> {
                    // Chat closes after command execution; open on the next tick so it stays open.
                    settingsRequested = true;
                    return 1;
                })));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (settingsRequested) {
                settingsRequested = false;
                if (client.level != null && client.player != null) {
                    client.gui.setScreen(createSettingsScreen(null));
                }
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

    public static InteractionSettingsScreen createSettingsScreen(Screen parent) {
        return new InteractionSettingsScreen(parent, instance.config, () -> {
            tracker.clear();
            instance.outlines = List.of();
        });
    }
}
