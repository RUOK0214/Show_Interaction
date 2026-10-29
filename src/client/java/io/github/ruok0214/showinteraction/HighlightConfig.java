package io.github.ruok0214.showinteraction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;

public final class HighlightConfig extends HighlightPreferences {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public int maxHighlights = 64;
    public boolean showWhenHudHidden = true;

    private static Path configFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("show-interaction.json");
    }

    public static HighlightConfig load() {
        Path FILE = configFile();
        if (!Files.exists(FILE)) {
            var config = new HighlightConfig();
            config.save();
            return config;
        }
        try {
            return decode(Files.readString(FILE));
        } catch (Exception error) {
            LoggerFactory.getLogger("show_interaction").warn("Cannot read {}; using defaults without overwriting it", FILE, error);
            return new HighlightConfig();
        }
    }

    static HighlightConfig decode(String contents) {
        var json = JsonParser.parseString(contents).getAsJsonObject();
        var config = GSON.fromJson(json, HighlightConfig.class);
        config.normalize();
        if (!json.has("reactions") && json.has("durationSeconds")) {
            config.migrateDuration(json.get("durationSeconds").getAsDouble());
        }
        var rows = json.has("reactions") && json.get("reactions").isJsonObject()
                ? json.getAsJsonObject("reactions") : null;
        for (var action : HighlightHistory.Action.values()) {
            var row = rows != null && rows.has(action.name()) && rows.get(action.name()).isJsonObject()
                    ? rows.getAsJsonObject(action.name()) : null;
            var reaction = config.reactions.get(action);
            if ((row == null || !row.has("lineWidth")) && json.has("lineWidth")) {
                try { reaction.lineWidth = json.get("lineWidth").getAsFloat(); }
                catch (RuntimeException ignored) { reaction.lineWidth = 3.0f; }
            }
            String legacyColor = action.name().toLowerCase(java.util.Locale.ROOT) + "Color";
            if ((row == null || !row.has("color")) && json.has(legacyColor)) {
                try { reaction.color = json.get(legacyColor).getAsString(); }
                catch (RuntimeException ignored) { reaction.color = defaultColor(action); }
            }
        }
        config.normalize();
        config.maxHighlights = Math.clamp(config.maxHighlights, 1, 256);
        return config;
    }

    String encode() { return GSON.toJson(this) + System.lineSeparator(); }

    public boolean save() {
        Path FILE = configFile();
        Path temporary = null;
        try {
            Files.createDirectories(FILE.getParent());
            temporary = Files.createTempFile(FILE.getParent(), "show-interaction-", ".tmp");
            Files.writeString(temporary, encode());
            try {
                Files.move(temporary, FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException error) {
            LoggerFactory.getLogger("show_interaction").warn("Cannot save {}", FILE, error);
            return false;
        } finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
        }
    }

}
