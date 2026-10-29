package io.github.ruok0214.showinteraction;

import static io.github.ruok0214.showinteraction.HighlightHistory.Action.*;

public final class HighlightHistoryTest {
    public static void main(String[] args) {
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
        System.out.println("Highlight checks passed: per-reaction timing, settings isolation, toggles, transparency, migration, expiry, recency, container hold and bounds.");
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
