package io.github.ruok0214.showinteraction;

import java.util.EnumMap;
import java.util.Locale;

/** UI-editable settings, independent of Minecraft and disk access. */
public class HighlightPreferences {
    public static final class Reaction {
        public boolean enabled = true;
        public boolean logEnabled = true;
        public double durationSeconds = 2.5;
        public float lineWidth = 3.0f;
        public String color;
    }

    public boolean enabled = true;
    public int transparencyPercent = 0;
    public EnumMap<HighlightHistory.Action, Reaction> reactions = defaults();

    private static EnumMap<HighlightHistory.Action, Reaction> defaults() {
        var result = new EnumMap<HighlightHistory.Action, Reaction>(HighlightHistory.Action.class);
        for (var action : HighlightHistory.Action.values()) {
            var reaction = new Reaction();
            reaction.color = defaultColor(action);
            result.put(action, reaction);
        }
        return result;
    }

    public static String defaultColor(HighlightHistory.Action action) {
        return switch (action) {
            case PLACE -> "#73F59A";
            case BREAK -> "#FF7777";
            case ADJUST -> "#FFD166";
            case OPEN -> "#63D9FF";
            case INSPECT -> "#D4B1FF";
        };
    }

    public static boolean validColor(String color) {
        return color != null && color.matches("#[0-9a-fA-F]{6}");
    }

    public void normalize() {
        transparencyPercent = Math.clamp(transparencyPercent, 0, 100);
        if (reactions == null) reactions = defaults();
        for (var action : HighlightHistory.Action.values()) {
            if (reactions.get(action) == null) reactions.put(action, new Reaction());
            var reaction = reactions.get(action);
            reaction.durationSeconds = Double.isFinite(reaction.durationSeconds)
                    ? Math.clamp(reaction.durationSeconds, 0.5, 10.0) : 2.5;
            reaction.lineWidth = Float.isFinite(reaction.lineWidth)
                    ? Math.clamp(reaction.lineWidth, 1.0f, 15.0f) : 3.0f;
            reaction.color = validColor(reaction.color) ? reaction.color.toUpperCase(Locale.ROOT) : defaultColor(action);
        }
    }

    public void migrateDuration(double oldDuration) {
        for (var reaction : reactions.values()) reaction.durationSeconds = oldDuration;
        normalize();
    }

    public boolean allows(HighlightHistory.Action action) { return enabled && reactions.get(action).enabled; }
    public boolean logs(HighlightHistory.Action action) { return reactions.get(action).logEnabled; }
    public long durationMillis(HighlightHistory.Action action) { return Math.round(reactions.get(action).durationSeconds * 1000); }
    public float opacityMultiplier() { return 1.0f - transparencyPercent / 100.0f; }
    public float lineWidth(HighlightHistory.Action action) { return reactions.get(action).lineWidth; }
    public int color(HighlightHistory.Action action) { return Integer.parseInt(reactions.get(action).color.substring(1), 16); }

    public HighlightPreferences copy() {
        var copy = new HighlightPreferences();
        copy.copyFrom(this);
        return copy;
    }

    public void copyFrom(HighlightPreferences source) {
        enabled = source.enabled;
        transparencyPercent = source.transparencyPercent;
        reactions = defaults();
        for (var action : HighlightHistory.Action.values()) {
            var original = source.reactions.get(action);
            reactions.get(action).enabled = original.enabled;
            reactions.get(action).logEnabled = original.logEnabled;
            reactions.get(action).durationSeconds = original.durationSeconds;
            reactions.get(action).lineWidth = original.lineWidth;
            reactions.get(action).color = original.color;
        }
        normalize();
    }
}
