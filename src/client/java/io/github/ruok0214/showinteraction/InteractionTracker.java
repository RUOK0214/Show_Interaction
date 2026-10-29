package io.github.ruok0214.showinteraction;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static io.github.ruok0214.showinteraction.HighlightHistory.Action.*;

/** Only direct local-player actions enter this tracker; unrelated world updates never do. */
public final class InteractionTracker {
    public record BlockSnapshot(BlockPos pos, BlockState state, List<AABB> boxes) { }
    public static final class UseAttempt {
        final ClientLevel level;
        final BlockSnapshot before;
        final long time;
        boolean placed;
        UseAttempt(ClientLevel level, BlockSnapshot before, long time) {
            this.level = level;
            this.before = before;
            this.time = time;
        }
    }
    private record Pending(BlockSnapshot before, long expires) { }
    private record Predicted(BlockState before, long expires) { }
    public record Outline(List<AABB> boxes, int color, float width) { }

    private final HighlightConfig config;
    private final HighlightHistory<BlockPos, List<AABB>> history;
    private final LinkedHashMap<BlockPos, Pending> pending = new LinkedHashMap<>();
    private final LinkedHashMap<BlockPos, Predicted> predicted = new LinkedHashMap<>();
    private ClientLevel level;
    private UseAttempt activeUse;
    private UseAttempt lastUse;
    private BlockPos openPos;
    private AbstractContainerMenu openMenu;

    public InteractionTracker(HighlightConfig config) {
        this.config = config;
        history = new HighlightHistory<>(config.maxHighlights, Math.round(config.durationSeconds * 1000));
    }

    private static long now() { return System.nanoTime() / 1_000_000; }

    private boolean ready() {
        Minecraft mc = Minecraft.getInstance();
        if (level != mc.level) {
            clear();
            level = mc.level;
        }
        return config.enabled && level != null && mc.player != null;
    }

    public void clear() {
        history.clear();
        pending.clear();
        predicted.clear();
        activeUse = null;
        lastUse = null;
        openPos = null;
        openMenu = null;
    }

    public BlockSnapshot capture(BlockPos pos) {
        if (!ready()) return null;
        BlockPos immutable = pos.immutable();
        BlockState state = level.getBlockState(immutable);
        var shape = state.getShape(level, immutable);
        List<AABB> boxes = new ArrayList<>();
        // Empty shapes (e.g. fluids) still get a visible indication of the interacted cell.
        if (shape.isEmpty()) boxes.add(new AABB(immutable).inflate(0.003));
        else for (AABB box : shape.toAabbs()) boxes.add(box.move(immutable).inflate(0.003));
        return new BlockSnapshot(immutable, state, List.copyOf(boxes));
    }

    public UseAttempt beginUse(BlockPos pos) {
        BlockSnapshot before = capture(pos);
        if (before == null) return null;
        activeUse = new UseAttempt(level, before, now());
        // An unrelated later click must not be mistaken for opening the previous chest.
        lastUse = null;
        return activeUse;
    }

    public void endUse(UseAttempt attempt, InteractionResult result) {
        activeUse = null;
        if (attempt == null || !ready() || attempt.level != level || attempt.placed) return;
        if (!result.consumesAction() || attempt.before.state().isAir()) return;
        BlockSnapshot after = capture(attempt.before.pos());
        if (after == null) return;
        lastUse = attempt;
        if (!attempt.before.state().equals(after.state())) {
            mark(after, ADJUST);
        } else {
            mark(after, INSPECT);
            pending.put(after.pos(), new Pending(attempt.before, now() + 1500));
            trim(pending);
        }
    }

    public void placed(BlockPos pos, BlockState before) {
        BlockSnapshot after = capture(pos);
        if (after == null || after.state().isAir() || before.equals(after.state())) return;
        if (activeUse != null) activeUse.placed = true;
        lastUse = null;
        pending.remove(pos);
        // Adding another slab/candle/snow layer changes an existing block in place.
        mark(after, before.is(after.state().getBlock()) ? ADJUST : PLACE);
        predicted.put(after.pos(), new Predicted(before, now() + 2000));
        trim(predicted);
    }

    public void broken(BlockSnapshot before, boolean success) {
        if (before == null || !success || !ready() || before.state().isAir()) return;
        if (before.state().equals(level.getBlockState(before.pos()))) return;
        pending.remove(before.pos());
        mark(before, BREAK);
        predicted.put(before.pos(), new Predicted(before.state(), now() + 2000));
        trim(predicted);
    }

    public void inspected(BlockPos pos) {
        BlockSnapshot snapshot = capture(pos);
        if (snapshot != null && !snapshot.state().isAir()) mark(snapshot, INSPECT);
    }

    /** Called only after the server has actually opened a block container screen. */
    public void containerOpened() {
        if (!ready() || lastUse == null || lastUse.level != level || now() - lastUse.time > 1500) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player.containerMenu == mc.player.inventoryMenu) return;
        BlockSnapshot snapshot = capture(lastUse.before.pos());
        // Ignore e.g. remote plugin menus when the clicked block has disappeared.
        if (snapshot == null || snapshot.state().isAir()) return;
        mark(snapshot, OPEN);
        pending.remove(snapshot.pos());
        openPos = snapshot.pos();
        openMenu = mc.player.containerMenu;
        lastUse = null;
    }

    public void tick() {
        if (!ready()) { clear(); return; }
        long time = now();
        if (lastUse != null && time - lastUse.time > 1500) lastUse = null;
        for (var iterator = pending.entrySet().iterator(); iterator.hasNext();) {
            var entry = iterator.next();
            if (time >= entry.getValue().expires()) { iterator.remove(); continue; }
            BlockState current = level.getBlockState(entry.getKey());
            if (!current.equals(entry.getValue().before().state())) {
                if (!current.isAir()) mark(capture(entry.getKey()), ADJUST);
                iterator.remove();
            }
        }
        // Client prediction can be rolled back by a protected multiplayer server.
        for (var iterator = predicted.entrySet().iterator(); iterator.hasNext();) {
            var entry = iterator.next();
            if (time >= entry.getValue().expires()) { iterator.remove(); continue; }
            if (level.getBlockState(entry.getKey()).equals(entry.getValue().before())) {
                history.remove(entry.getKey());
                iterator.remove();
            }
        }
        if (openPos != null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player.containerMenu == openMenu && mc.gui.screen() != null) history.hold(openPos, time);
            else { openPos = null; openMenu = null; }
        }
        history.snapshot(time);
    }

    public List<Outline> extract() {
        if (!ready()) return List.of();
        Minecraft mc = Minecraft.getInstance();
        if (!config.showWhenHudHidden && mc.gui.hud.isHidden()) return List.of();
        long time = now();
        List<HighlightHistory.Entry<BlockPos, List<AABB>>> entries = history.snapshot(time);
        List<Outline> result = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            if (mc.player.distanceToSqr(entry.key().getX() + 0.5, entry.key().getY() + 0.5, entry.key().getZ() + 0.5) > 4096) continue;
            boolean latest = i == entries.size() - 1;
            int alpha = Math.round(255 * entry.opacity(time) * (latest ? 1f : 0.65f));
            if (alpha == 0) continue;
            result.add(new Outline(entry.geometry(), (alpha << 24) | config.color(entry.action()), config.lineWidth * (latest ? 1f : 0.75f)));
        }
        return List.copyOf(result);
    }

    private void mark(BlockSnapshot snapshot, HighlightHistory.Action action) {
        if (snapshot != null) history.mark(snapshot.pos(), snapshot.boxes(), action, now());
    }
    private void trim(LinkedHashMap<?, ?> map) {
        while (map.size() > config.maxHighlights) map.remove(map.keySet().iterator().next());
    }
}
