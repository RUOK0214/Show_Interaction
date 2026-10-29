package io.github.ruok0214.showinteraction;

import static io.github.ruok0214.showinteraction.HighlightHistory.Action.*;

public final class HighlightHistoryTest {
    public static void main(String[] args) throws Exception {
        var history = new HighlightHistory<String, String>(3);
        history.mark("repeater", "low shape", ADJUST, 100, 2500);
        var entry = history.snapshot(100).getFirst();
        check(entry.opacity(100) == 1, "Starts fully visible");
        check(entry.opacity(2400) > 0 && entry.opacity(2400) < 0.3, "Fades before expiry");
        check(history.snapshot(2599).size() == 1, "Lives for configured duration");
        check(history.snapshot(2600).isEmpty(), "Expires at exact deadline");

        history.mark("a", "original removed shape", BREAK, 3000, 2500);
        history.mark("b", "chest", OPEN, 3100, 2500);
        history.mark("a", "new shape", PLACE, 3200, 2500);
        var snapshot = history.snapshot(3200);
        check(snapshot.size() == 2 && snapshot.getLast().key().equals("a"), "Repeated position becomes latest without duplication");
        check(snapshot.getLast().action() == PLACE && snapshot.getLast().geometry().equals("new shape"), "Newest geometry and action replace old values");
        history.mark("c", "c", INSPECT, 3300, 2500);
        history.mark("d", "d", PLACE, 3400, 2500);
        check(!history.contains("b") && history.contains("a"), "Evicts oldest interaction, not first inserted position");
        check(snapshot.size() == 2, "Previous snapshot stays immutable");

        history.clear();
        history.mark("chest", "shape", OPEN, 10000, 2500);
        history.hold("chest", 12000);
        check(history.snapshot(14000).size() == 1, "Open container extends visibility");
        check(history.snapshot(14000).getFirst().started() == 10000, "Holding does not reset pulse");
        check(history.snapshot(14500).isEmpty(), "Expires after closing the container");
        history.hold("missing", 15000);
        check(history.snapshot(15000).isEmpty(), "Holding does not recreate removed highlights");

        history.mark("denied", "shape", PLACE, 20000, 2500);
        history.remove("denied");
        check(history.snapshot(20000).isEmpty(), "Rejected prediction can be removed");
        history.mark("old world", "shape", PLACE, 21000, 2500);
        history.clear();
        check(history.snapshot(21000).isEmpty(), "World/toggle reset clears history");
        for (int i = 0; i < 10000; i++) history.mark("block" + i, "shape", PLACE, 22000, 2500);
        check(history.snapshot(22000).size() == 3, "Rapid actions stay bounded");
        for (long time = 22000; time < 24500; time++) {
            float opacity = history.snapshot(time).getFirst().opacity(time);
            check(Float.isFinite(opacity) && opacity >= 0 && opacity <= 1, "Opacity always within range");
        }
        history.clear();
        history.mark("short", "shape", PLACE, 30000, 500);
        history.mark("long", "shape", BREAK, 30000, 10000);
        check(history.snapshot(30500).size() == 1 && history.contains("long"), "Each reaction has its own deadline");
        history.mark("chest", "shape", OPEN, 31000, 1000);
        history.hold("chest", 34000);
        check(history.snapshot(34999).size() == 2, "Holding preserves this container's configured duration");
        check(history.snapshot(35000).size() == 1, "Container expires using its own duration");
        history.mark("long", "shape", BREAK, 36000, 500);
        check(history.snapshot(36500).isEmpty(), "New duration replaces previous duration at the same position");
        preferences();
        appearance();
        configMigration();
        InteractionLogTest.run();
        System.out.println("Highlight checks passed: per-reaction timing/color/width, draft isolation, config migration/round trip, toggles, transparency, expiry, recency, container hold and bounds.");
    }

    private static void appearance() {
        var prefs = new HighlightPreferences();
        check(prefs.color(PLACE) == 0x73F59A && prefs.color(BREAK) == 0xFF7777, "Original default colors are unchanged");
        var draft = prefs.copy();
        draft.reactions.get(PLACE).color = "#112233";
        draft.reactions.get(PLACE).lineWidth = 7.5f;
        draft.reactions.get(BREAK).color = "#abcdef";
        draft.reactions.get(BREAK).lineWidth = 1;
        check(prefs.lineWidth(PLACE) == 3 && prefs.color(PLACE) == 0x73F59A, "Appearance drafts do not leak before saving");
        prefs.copyFrom(draft);
        check(prefs.lineWidth(PLACE) == 7.5f && prefs.color(PLACE) == 0x112233, "Place appearance applies independently");
        check(prefs.lineWidth(BREAK) == 1 && prefs.color(BREAK) == 0xABCDEF, "Break appearance applies independently");
        check(prefs.lineWidth(OPEN) == 3 && prefs.color(OPEN) == 0x63D9FF, "Other reactions remain unchanged");
        check(prefs.reactions.get(BREAK).color.equals("#ABCDEF"), "Color representation is normalized");
        draft.reactions.get(PLACE).color = "#000000";
        draft.reactions.get(PLACE).lineWidth = 2;
        check(prefs.color(PLACE) == 0x112233 && prefs.lineWidth(PLACE) == 7.5f, "Saved appearance does not share mutable draft rows");
        var previous = prefs.copy();
        prefs.copyFrom(draft);
        prefs.copyFrom(previous);
        check(prefs.color(PLACE) == 0x112233 && prefs.lineWidth(PLACE) == 7.5f, "Failed-save rollback restores appearance");
        prefs.reactions.get(PLACE).lineWidth = Float.NaN;
        prefs.reactions.get(BREAK).lineWidth = 100;
        prefs.reactions.get(OPEN).lineWidth = -1;
        prefs.reactions.get(PLACE).color = "#11223344";
        prefs.reactions.get(BREAK).color = null;
        prefs.reactions.get(OPEN).color = "#000000";
        prefs.normalize();
        check(prefs.lineWidth(PLACE) == 3 && prefs.lineWidth(BREAK) == 15 && prefs.lineWidth(OPEN) == 1, "Invalid widths are safe");
        check(prefs.color(PLACE) == 0x73F59A && prefs.color(BREAK) == 0xFF7777 && prefs.color(OPEN) == 0, "Invalid colors recover per-action defaults; black stays valid");
    }

    private static void configMigration() {
        var legacy = HighlightConfig.decode("""
                {"durationSeconds":4.5,"lineWidth":5.5,"placeColor":"#123abc","breakColor":"#222222",
                 "adjustColor":"#333333","openColor":"#444444","inspectColor":"#555555",
                 "maxHighlights":128,"showWhenHudHidden":false}
                """);
        int[] colors = {0x123ABC, 0x222222, 0x333333, 0x444444, 0x555555};
        for (var action : HighlightHistory.Action.values()) {
            check(legacy.lineWidth(action) == 5.5f && legacy.durationMillis(action) == 4500, "0.1 common settings migrate to each action");
            check(legacy.color(action) == colors[action.ordinal()], "Legacy action colors are preserved");
        }
        check(legacy.maxHighlights == 128 && !legacy.showWhenHudHidden, "Non-screen settings are preserved");
        var partial = HighlightConfig.decode("""
                {"enabled":false,"transparencyPercent":40,"lineWidth":6,"placeColor":"#112233","openColor":"#445566",
                 "reactions":{"PLACE":{"enabled":false,"durationSeconds":7,"color":"#abcdef","lineWidth":2},
                 "OPEN":{"durationSeconds":9},"BREAK":null}}
                """);
        check(!partial.enabled && partial.transparencyPercent == 40, "Global settings survive migration");
        check(partial.lineWidth(PLACE) == 2 && partial.color(PLACE) == 0xABCDEF, "New per-action fields take precedence over legacy fields");
        check(!partial.reactions.get(PLACE).enabled && partial.durationMillis(PLACE) == 7000, "Existing reaction toggles and times survive migration");
        check(partial.lineWidth(OPEN) == 6 && partial.color(OPEN) == 0x445566 && partial.durationMillis(OPEN) == 9000, "0.2 rows inherit missing appearance without losing their time");
        check(partial.lineWidth(BREAK) == 6 && partial.color(BREAK) == 0xFF7777, "Null legacy rows recover safely");
        String saved = partial.encode();
        check(!saved.contains("placeColor") && !saved.contains("openColor"), "New files have one source of truth for each color");
        var restored = HighlightConfig.decode(saved);
        for (var action : HighlightHistory.Action.values()) {
            check(restored.color(action) == partial.color(action) && restored.lineWidth(action) == partial.lineWidth(action), "Appearance survives JSON save and reload");
            check(restored.durationMillis(action) == partial.durationMillis(action)
                    && restored.reactions.get(action).enabled == partial.reactions.get(action).enabled, "Time and enabled state survive JSON save and reload");
        }
        var invalid = HighlightConfig.decode("""
                {"lineWidth":"invalid","placeColor":null,"openColor":"invalid",
                 "reactions":{"PLACE":{"color":"#12345","lineWidth":999},"OPEN":{"lineWidth":-3}}}
                """);
        check(invalid.lineWidth(PLACE) == 15 && invalid.color(PLACE) == 0x73F59A, "Invalid new appearance is normalized");
        check(invalid.lineWidth(OPEN) == 1 && invalid.color(OPEN) == 0x63D9FF && invalid.lineWidth(BREAK) == 3, "Invalid legacy values do not erase valid rows");
    }

    private static void preferences() {
        var prefs = new HighlightPreferences();
        check(prefs.opacityMultiplier() == 1, "Default is fully visible");
        prefs.transparencyPercent = 100;
        check(prefs.opacityMultiplier() == 0, "100% transparency is invisible");
        prefs.transparencyPercent = 35;
        check(Math.abs(prefs.opacityMultiplier() - 0.65f) < 0.0001f, "Transparency scales opacity");
        prefs.reactions.get(PLACE).enabled = false;
        check(!prefs.allows(PLACE) && prefs.allows(OPEN), "Individual toggles are independent");
        prefs.enabled = false;
        for (var action : HighlightHistory.Action.values()) check(!prefs.allows(action), "Global switch overrides each reaction");
        prefs.enabled = true;
        check(!prefs.allows(PLACE) && prefs.allows(OPEN), "Re-enabling global switch preserves per-reaction settings");
        var draft = prefs.copy();
        draft.reactions.get(OPEN).durationSeconds = 8;
        draft.reactions.get(ADJUST).enabled = false;
        check(prefs.durationMillis(OPEN) == 2500 && prefs.allows(ADJUST), "Unsaved screen edits are isolated");
        prefs.copyFrom(draft);
        check(prefs.durationMillis(OPEN) == 8000 && !prefs.allows(ADJUST), "Saving applies all screen edits");
        draft.reactions.get(OPEN).durationSeconds = 4;
        check(prefs.durationMillis(OPEN) == 8000, "Applied config does not share mutable draft rows");
        prefs.migrateDuration(4.5);
        for (var action : HighlightHistory.Action.values()) check(prefs.durationMillis(action) == 4500, "Legacy duration migrates to every reaction");
        prefs.transparencyPercent = -99;
        prefs.reactions.put(OPEN, null);
        prefs.reactions.get(PLACE).durationSeconds = Double.NaN;
        prefs.reactions.get(BREAK).durationSeconds = 999;
        prefs.reactions.get(ADJUST).durationSeconds = -1;
        prefs.normalize();
        check(prefs.transparencyPercent == 0 && prefs.durationMillis(OPEN) == 2500, "Missing settings recover defaults");
        check(prefs.durationMillis(PLACE) == 2500 && prefs.durationMillis(BREAK) == 10000 && prefs.durationMillis(ADJUST) == 500, "Invalid durations are bounded");
        prefs.reactions = null;
        prefs.transparencyPercent = 200;
        prefs.normalize();
        check(prefs.reactions.size() == 5 && prefs.transparencyPercent == 100, "Null map and out-of-range transparency recover");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
