package io.github.ruok0214.showinteraction;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static io.github.ruok0214.showinteraction.HighlightHistory.Action.*;

final class InteractionLogTest {
    static void run() throws Exception {
        var properties = new HashMap<>(Map.of("delay", "1"));
        var before = new InteractionLog.Block("minecraft:repeater", "block.minecraft.repeater", properties);
        properties.put("delay", "4");
        check(before.properties().get("delay").equals("1"), "Block details do not share mutable maps");
        var after = new InteractionLog.Block("minecraft:repeater", "block.minecraft.repeater", Map.of("delay", "2"));
        var log = new InteractionLog();
        long first = log.add(100, ADJUST, "Test world", "minecraft:overworld", 1, 2, 3, before, after);
        var oldSnapshot = log.snapshot();
        long second = log.add(200, ADJUST, "Test world", "minecraft:overworld", 1, 2, 3, after, before);
        check(log.snapshot().size() == 2 && log.newestFirst().getFirst().id() == second, "Repeated positions remain separate events in newest-first order");
        log.markReverted(first);
        check(log.snapshot().getFirst().reverted() && !log.snapshot().getLast().reverted(), "Rollback updates only the corresponding event");
        check(!oldSnapshot.getFirst().reverted(), "Open detail snapshots remain immutable");
        long revision = log.revision();
        log.markReverted(0);
        log.markReverted(first);
        check(revision == log.revision(), "Missing and repeated rollback updates are ignored");
        for (int i = 0; i < 1100; i++) log.add(300 + i, PLACE, "Second world", "minecraft:the_nether", i, 64, 0, before, after);
        check(log.snapshot().size() == 1000 && log.newestFirst().getFirst().x() == 1099, "History is bounded and keeps latest events across worlds");
        check(log.snapshot().getFirst().x() == 100, "Oldest entries are evicted exactly at the limit");

        var prefs = new HighlightPreferences();
        prefs.enabled = false;
        prefs.reactions.get(PLACE).enabled = false;
        check(prefs.logs(PLACE), "Logging works while global and per-action highlights are off");
        var draft = prefs.copy();
        draft.reactions.get(PLACE).logEnabled = false;
        check(prefs.logs(PLACE), "Unsaved logging switches are isolated");
        prefs.copyFrom(draft);
        check(!prefs.logs(PLACE) && prefs.logs(OPEN), "Each logging switch applies independently");
        var config = HighlightConfig.decode("{\"reactions\":{\"PLACE\":{\"logEnabled\":false,\"lineWidth\":14.5}}}");
        var configReload = HighlightConfig.decode(config.encode());
        check(!configReload.logs(PLACE) && configReload.logs(OPEN) && configReload.lineWidth(PLACE) == 14.5f,
                "Log switches and widths above eight survive save and reload");

        Path directory = Files.createTempDirectory(Path.of("build"), "interaction-log-test-");
        Path file = directory.resolve("history.json");
        try (var store = new InteractionLogStore(file, log)) {
            log.add(2000, BREAK, "Test world", "minecraft:overworld", 9, 70, 11, before, after);
            store.tick();
            log.add(2001, OPEN, "Test world", "minecraft:overworld", 10, 70, 11, before, after);
        }
        var restored = new InteractionLog();
        try (var store = new InteractionLogStore(file, restored)) {
            check(!store.failed() && restored.snapshot().equals(log.snapshot()), "Shutdown flush persists edits arriving during a background write");
            long lastId = restored.newestFirst().getFirst().id();
            long next = restored.add(2002, INSPECT, "Test world", "minecraft:overworld", 11, 70, 11, before, after);
            check(next > lastId, "IDs stay unique across restarts");
        }
        var corrupt = directory.resolve("corrupt.json");
        Files.writeString(corrupt, "not valid json");
        try (var store = new InteractionLogStore(corrupt, new InteractionLog())) {
            check(!store.failed(), "A corrupt file can be preserved and recovered");
            try (var files = Files.list(directory)) {
                check(files.anyMatch(path -> path.getFileName().toString().startsWith("corrupt.json.invalid-")), "Corrupt history is backed up before replacement");
            }
        }
        System.out.println("Log checks passed: independent switches, detail snapshots, rollback, retention, persistence and recovery.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
