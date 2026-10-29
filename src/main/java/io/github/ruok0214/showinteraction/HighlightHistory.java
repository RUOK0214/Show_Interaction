package io.github.ruok0214.showinteraction;

import java.util.LinkedHashMap;
import java.util.List;

/** Bounded, deterministic history. All times use the same monotonic clock. */
public final class HighlightHistory<K, V> {
    public enum Action { PLACE, BREAK, ADJUST, OPEN, INSPECT }
    public record Entry<K, V>(K key, V geometry, Action action, long started, long expires, long duration) {
        public float opacity(long now) {
            if (now >= expires) return 0;
            double age = Math.max(0, now - started) / 1000.0;
            double pulse = 0.78 + 0.22 * Math.cos(age * Math.PI * 4);
            double fade = Math.min(1, (expires - now) / Math.min(800.0, duration));
            return (float) (pulse * fade);
        }
    }
    private final LinkedHashMap<K, Entry<K, V>> entries = new LinkedHashMap<>();
    private final int capacity;

    public HighlightHistory(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("Invalid history bounds");
        this.capacity = capacity;
    }

    public void mark(K key, V geometry, Action action, long now, long duration) {
        if (duration < 1) throw new IllegalArgumentException("Invalid highlight duration");
        prune(now);
        entries.remove(key);
        entries.put(key, new Entry<>(key, geometry, action, now, now + duration, duration));
        while (entries.size() > capacity) entries.remove(entries.keySet().iterator().next());
    }

    /** Keeps an open container visible, without restarting its pulse every frame. */
    public void hold(K key, long now) {
        var entry = entries.get(key);
        if (entry != null) entries.put(key, new Entry<>(key, entry.geometry(), entry.action(), entry.started(), now + entry.duration(), entry.duration()));
    }

    public void remove(K key) { entries.remove(key); }
    public void clear() { entries.clear(); }
    public boolean contains(K key) { return entries.containsKey(key); }
    public List<Entry<K, V>> snapshot(long now) {
        prune(now);
        return List.copyOf(entries.values());
    }
    private void prune(long now) { entries.values().removeIf(entry -> now >= entry.expires()); }
}
