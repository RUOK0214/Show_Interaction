package io.github.ruok0214.showinteraction;

import static io.github.ruok0214.showinteraction.HighlightHistory.Action.*;

public final class HighlightHistoryTest {
    public static void main(String[] args) {
        var history = new HighlightHistory<String, String>(3, 2500);
        history.mark("repeater", "low shape", ADJUST, 100);
        var entry = history.snapshot(100).getFirst();
        check(entry.opacity(100) == 1, "Starts fully visible");
        check(entry.opacity(2400) > 0 && entry.opacity(2400) < 0.3, "Fades before expiry");
        check(history.snapshot(2599).size() == 1, "Lives for configured duration");
        check(history.snapshot(2600).isEmpty(), "Expires at exact deadline");

        history.mark("a", "original removed shape", BREAK, 3000);
        history.mark("b", "chest", OPEN, 3100);
        history.mark("a", "new shape", PLACE, 3200);
        var snapshot = history.snapshot(3200);
        check(snapshot.size() == 2 && snapshot.getLast().key().equals("a"), "Repeated position becomes latest without duplication");
        check(snapshot.getLast().action() == PLACE && snapshot.getLast().geometry().equals("new shape"), "Newest geometry and action replace old values");
        history.mark("c", "c", INSPECT, 3300);
        history.mark("d", "d", PLACE, 3400);
        check(!history.contains("b") && history.contains("a"), "Evicts oldest interaction, not first inserted position");
        check(snapshot.size() == 2, "Previous snapshot stays immutable");

        history.clear();
        history.mark("chest", "shape", OPEN, 10000);
        history.hold("chest", 12000);
        check(history.snapshot(14000).size() == 1, "Open container extends visibility");
        check(history.snapshot(14000).getFirst().started() == 10000, "Holding does not reset pulse");
        check(history.snapshot(14500).isEmpty(), "Expires after closing the container");
        history.hold("missing", 15000);
        check(history.snapshot(15000).isEmpty(), "Holding does not recreate removed highlights");

        history.mark("denied", "shape", PLACE, 20000);
        history.remove("denied");
        check(history.snapshot(20000).isEmpty(), "Rejected prediction can be removed");
        history.mark("old world", "shape", PLACE, 21000);
        history.clear();
        check(history.snapshot(21000).isEmpty(), "World/toggle reset clears history");
        for (int i = 0; i < 10000; i++) history.mark("block" + i, "shape", PLACE, 22000);
        check(history.snapshot(22000).size() == 3, "Rapid actions stay bounded");
        for (long time = 22000; time < 24500; time++) {
            float opacity = history.snapshot(time).getFirst().opacity(time);
            check(Float.isFinite(opacity) && opacity >= 0 && opacity <= 1, "Opacity always within range");
        }
        System.out.println("Highlight history checks passed: expiry, fade, replacement, recency, capacity, container hold, rollback and reset.");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
