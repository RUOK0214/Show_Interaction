package io.github.ruok0214.showinteraction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HighlightConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("show-interaction.json");
    public boolean enabled = true;
    public double durationSeconds = 2.5;
    public int maxHighlights = 64;
    public float lineWidth = 3.0f;
    public boolean showWhenHudHidden = true;
    public String placeColor = "#73F59A";
    public String breakColor = "#FF7777";
    public String adjustColor = "#FFD166";
    public String openColor = "#63D9FF";
    public String inspectColor = "#D4B1FF";

    public static HighlightConfig load() {
        if (!Files.exists(FILE)) {
            var config = new HighlightConfig();
            config.save();
            return config;
        }
        try (var reader = Files.newBufferedReader(FILE)) {
            var config = GSON.fromJson(reader, HighlightConfig.class);
            if (config == null) throw new IOException("Empty config");
            config.durationSeconds = Double.isFinite(config.durationSeconds)
                    ? Math.clamp(config.durationSeconds, 0.5, 10.0) : 2.5;
            config.maxHighlights = Math.clamp(config.maxHighlights, 1, 256);
            config.lineWidth = Float.isFinite(config.lineWidth) ? Math.clamp(config.lineWidth, 1.0f, 8.0f) : 3.0f;
            return config;
        } catch (Exception error) {
            LoggerFactory.getLogger("show_interaction").warn("Cannot read {}; using defaults without overwriting it", FILE, error);
            return new HighlightConfig();
        }
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(this) + System.lineSeparator());
        } catch (IOException error) {
            LoggerFactory.getLogger("show_interaction").warn("Cannot save {}", FILE, error);
        }
    }

    public int color(HighlightHistory.Action action) {
        String value = switch (action) {
            case PLACE -> placeColor;
            case BREAK -> breakColor;
            case ADJUST -> adjustColor;
            case OPEN -> openColor;
            case INSPECT -> inspectColor;
        };
        int fallback = switch (action) {
            case PLACE -> 0x73F59A;
            case BREAK -> 0xFF7777;
            case ADJUST -> 0xFFD166;
            case OPEN -> 0x63D9FF;
            case INSPECT -> 0xD4B1FF;
        };
        if (value == null || !value.matches("#[0-9a-fA-F]{6}")) return fallback;
        return Integer.parseInt(value.substring(1), 16);
    }
}
