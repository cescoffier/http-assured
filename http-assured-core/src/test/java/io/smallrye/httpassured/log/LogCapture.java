package io.smallrye.httpassured.log;

import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test utility that installs a recording {@link Logger} into {@link RequestLogger}
 * and {@link ResponseLogger}, capturing every log call made during a test.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * LogCapture capture = LogCapture.install();
 * try {
 *     // ... exercise code that logs ...
 *     assertTrue(capture.hasInfo("Request"));
 *     assertTrue(capture.hasError("Response"));
 * } finally {
 *     capture.uninstall();
 * }
 * }</pre>
 *
 * <p>Always call {@link #uninstall()} in a {@code finally} block or
 * {@code @AfterEach} to restore the real loggers.</p>
 */
public final class LogCapture {

    /**
     * A recorded log entry.
     *
     * @param level   JBoss Logging level (INFO, ERROR, …)
     * @param message the formatted message
     */
    public record Entry(Logger.Level level, String message) {}

    private final List<Entry> entries = Collections.synchronizedList(new ArrayList<>());
    private final Logger recordingLogger;

    private LogCapture() {
        // Extend Logger anonymously — all log methods delegate here
        this.recordingLogger = new RecordingLogger(this);
    }

    /**
     * Installs a recording logger in both {@link RequestLogger} and
     * {@link ResponseLogger} and returns the capture handle.
     *
     * @return the active capture handle — call {@link #uninstall()} when done
     */
    public static LogCapture install() {
        LogCapture capture = new LogCapture();
        RequestLogger.setLogger(capture.recordingLogger);
        ResponseLogger.setLogger(capture.recordingLogger);
        return capture;
    }

    /**
     * Restores the real JBoss Logging loggers in both {@link RequestLogger} and
     * {@link ResponseLogger}.
     */
    public void uninstall() {
        Logger real = Logger.getLogger("io.smallrye.httpassured.log");
        RequestLogger.setLogger(real);
        ResponseLogger.setLogger(real);
    }

    /** Clears all captured entries. */
    public void reset() {
        entries.clear();
    }

    /** Returns an unmodifiable snapshot of all captured entries. */
    public List<Entry> entries() {
        return List.copyOf(entries);
    }

    /**
     * Returns {@code true} if any INFO-level entry contains {@code text}.
     *
     * @param text substring to look for
     */
    public boolean hasInfo(String text) {
        return entries.stream()
                .filter(e -> e.level() == Logger.Level.INFO)
                .anyMatch(e -> e.message() != null && e.message().contains(text));
    }

    /**
     * Returns {@code true} if any ERROR-level entry contains {@code text}.
     *
     * @param text substring to look for
     */
    public boolean hasError(String text) {
        return entries.stream()
                .filter(e -> e.level() == Logger.Level.ERROR)
                .anyMatch(e -> e.message() != null && e.message().contains(text));
    }

    /** Returns {@code true} if no entry at any level contains {@code text}. */
    public boolean hasNone(String text) {
        return entries.stream().noneMatch(e -> e.message() != null && e.message().contains(text));
    }


    void record(Logger.Level level, Object message) {
        entries.add(new Entry(level, message != null ? message.toString() : null));
    }

    /**
     * Minimal {@link Logger} subclass that records every log call into the
     * owning {@link LogCapture} rather than emitting to any backend.
     */
    private static final class RecordingLogger extends Logger {

        private final LogCapture capture;

        RecordingLogger(LogCapture capture) {
            super("io.smallrye.httpassured.log.capture");
            this.capture = capture;
        }

        @Override
        protected void doLog(Level level, String loggerClassName, Object message, Object[] parameters, Throwable thrown) {
            capture.record(level, message);
        }

        @Override
        protected void doLogf(Level level, String loggerClassName, String format, Object[] parameters, Throwable thrown) {
            capture.record(level, format != null && parameters != null
                    ? String.format(format, parameters)
                    : format);
        }

        @Override
        public boolean isEnabled(Level level) {
            return true;
        }
    }
}
