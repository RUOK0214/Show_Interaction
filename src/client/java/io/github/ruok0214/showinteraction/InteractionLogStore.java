package io.github.ruok0214.showinteraction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Writes immutable snapshots off the game thread, at most once per second. */
public final class InteractionLogStore implements AutoCloseable {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private final InteractionLog log;
    private final ExecutorService writer = Executors.newSingleThreadExecutor(task -> {
        var thread = new Thread(task, "show-interaction-log");
        thread.setDaemon(true);
        return thread;
    });
    private volatile long savedRevision;
    private volatile boolean writing;
    private volatile boolean failed;
    private long nextWrite;

    public InteractionLogStore(Path file, InteractionLog log) {
        this.file = file;
        this.log = log;
        if (Files.exists(file)) {
            try {
                if (Files.size(file) > 8 * 1024 * 1024) throw new IOException("Log file exceeds size limit");
                var entries = GSON.fromJson(Files.readString(file), InteractionLog.Entry[].class);
                if (entries == null) throw new IOException("Empty log document");
                log.restore(Arrays.asList(entries));
            } catch (Exception error) {
                LoggerFactory.getLogger("show_interaction").warn("Cannot read interaction history {}", file, error);
                try {
                    Files.copy(file, file.resolveSibling(file.getFileName() + ".invalid-" + System.currentTimeMillis()));
                } catch (IOException backupError) {
                    // Do not overwrite a history file we could not read or preserve.
                    writer.shutdown();
                    failed = true;
                }
                log.restore(List.of());
            }
        }
        savedRevision = log.revision();
    }

    public boolean failed() { return failed; }

    public void tick() {
        long now = System.nanoTime();
        if (writer.isShutdown() || writing || log.revision() == savedRevision || now < nextWrite) return;
        nextWrite = now + TimeUnit.SECONDS.toNanos(1);
        writing = true;
        long revision = log.revision();
        var snapshot = log.snapshot();
        writer.execute(() -> {
            try { persist(snapshot, revision); }
            finally { writing = false; }
        });
    }

    private void persist(List<InteractionLog.Entry> snapshot, long revision) {
        Path temporary = null;
        try {
            Files.createDirectories(file.getParent());
            temporary = Files.createTempFile(file.getParent(), "show-interaction-log-", ".tmp");
            Files.writeString(temporary, GSON.toJson(snapshot) + System.lineSeparator());
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            savedRevision = revision;
            failed = false;
        } catch (IOException error) {
            if (!failed) LoggerFactory.getLogger("show_interaction").warn("Cannot save interaction history {}", file, error);
            failed = true;
        } finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
        }
    }

    @Override public void close() {
        if (writer.isShutdown()) return;
        var snapshot = log.snapshot();
        long revision = log.revision();
        if (revision != savedRevision) writer.execute(() -> persist(snapshot, revision));
        writer.shutdown();
        try {
            if (!writer.awaitTermination(5, TimeUnit.SECONDS)) {
                LoggerFactory.getLogger("show_interaction").warn("Interaction history writer did not finish before shutdown");
            }
        } catch (InterruptedException error) { Thread.currentThread().interrupt(); }
    }
}
