package io.github.ruok0214.showinteraction;

import java.util.EnumMap;

/** UI-editable settings, independent of Minecraft and disk access. */
public class HighlightPreferences {
    public static final class Reaction {
        public boolean enabled = true;
        public double durationSeconds = 2.5;
    }

    public boolean enabled = true;
    public int transparencyPercent = 0;
    public EnumMap<HighlightHistory.Action, Reaction> reactions = defaults();

    private static EnumMap<HighlightHistory.Action, Reaction> defaults() {
        var result = new EnumMap<HighlightHistory.Action, Reaction>(HighlightHistory.Action.class);
        for (var action : HighlightHistory.Action.values()) result.put(action, new Reaction());
        return result;
    }

    public void normalize() {
        transparencyPercent = Math.clamp(transparencyPercent, 0, 100);
        if (reactions == null) reactions = defaults();
        for (var action : HighlightHistory.Action.values()) {
            if (reactions.get(action) == null) reactions.put(action, new Reaction());
            var reaction = reactions.get(action);
            reaction.durationSeconds = Double.isFinite(reaction.durationSeconds)
                    ? Math.clamp(reaction.durationSeconds, 0.5, 10.0) : 2.5;
        }
    }

    public void migrateDuration(double oldDuration) {
        for (var reaction : reactions.values()) reaction.durationSeconds = oldDuration;
        normalize();
    }

    public boolean allows(HighlightHistory.Action action) { return enabled && reactions.get(action).enabled; }
    public long durationMillis(HighlightHistory.Action action) { return Math.round(reactions.get(action).durationSeconds * 1000); }
    public float opacityMultiplier() { return 1.0f - transparencyPercent / 100.0f; }

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
            reactions.get(action).durationSeconds = original.durationSeconds;
        }
        normalize();
    }
}
