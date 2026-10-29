package io.github.ruok0214.showinteraction;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Bounded local history. Entries are immutable so the UI and background writer can retain snapshots. */
public final class InteractionLog {
    public static final int LIMIT = 1000;

    public record Block(String id, String translationKey, Map<String, String> properties) {
        public Block {
            Objects.requireNonNull(id);
            Objects.requireNonNull(translationKey);
            properties = Map.copyOf(properties);
        }
    }

    public record Entry(long id, long timeMillis, HighlightHistory.Action action, String world, String dimension,
                        int x, int y, int z, Block before, Block after, boolean reverted) {
        public Entry {
            if (id <= 0 || timeMillis < 0) throw new IllegalArgumentException("Invalid log identity or timestamp");
            Objects.requireNonNull(action);
            Objects.requireNonNull(world);
            Objects.requireNonNull(dimension);
            Objects.requireNonNull(before);
            Objects.requireNonNull(after);
        }
        public Block displayBlock() { return action == HighlightHistory.Action.BREAK ? before : after; }
    }

    private final LinkedHashMap<Long, Entry> entries = new LinkedHashMap<>();
    private long nextId = 1;
    private long revision;

    public long add(long timeMillis, HighlightHistory.Action action, String world, String dimension,
                    int x, int y, int z, Block before, Block after) {
        long id = nextId++;
        entries.put(id, new Entry(id, timeMillis, action, world, dimension, x, y, z, before, after, false));
        trim();
        revision++;
        return id;
    }

    public void markReverted(long id) {
        var entry = entries.get(id);
        if (entry == null || entry.reverted()) return;
        entries.put(id, new Entry(entry.id(), entry.timeMillis(), entry.action(), entry.world(), entry.dimension(),
                entry.x(), entry.y(), entry.z(), entry.before(), entry.after(), true));
        revision++;
    }

    public List<Entry> snapshot() { return List.copyOf(entries.values()); }
    public List<Entry> newestFirst() { return snapshot().reversed(); }
    public long revision() { return revision; }

    public void restore(List<Entry> saved) {
        entries.clear();
        nextId = 1;
        for (var entry : saved) {
            if (entry.id() == Long.MAX_VALUE) throw new IllegalArgumentException("Invalid log ID");
            entries.put(entry.id(), entry);
            nextId = Math.max(nextId, entry.id() + 1);
            trim();
        }
        revision++;
    }

    private void trim() { while (entries.size() > LIMIT) entries.pollFirstEntry(); }
}
