package io.smallrye.httpassured.log;

import io.smallrye.httpassured.http.Header;
import io.smallrye.httpassured.spi.RawResponse;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Set;

/**
 * Stateless utility that formats and emits response log lines via JBoss Logging.
 * <p>
 * Normal output is at {@code INFO} level; error output (status &ge; 400 or
 * assertion failure) is at {@code ERROR} level. Both use the
 * {@code io.smallrye.httpassured.log} category.
 * Header values whose names appear in the blacklist are replaced with
 * {@code [ BLACKLISTED ]}.
 * </p>
 */
public final class ResponseLogger {

    private static volatile Logger LOG = Logger.getLogger("io.smallrye.httpassured.log");

    /**
     * Replaces the logger used by this class.
     * <p>
     * Package-visible — intended for test use only. Pass a recording logger
     * obtained from {@link LogCapture} to capture log output in tests.
     * </p>
     *
     * @param logger the replacement logger (must not be {@code null})
     */
    static void setLogger(Logger logger) {
        LOG = logger;
    }

    /** Replacement text used for blacklisted header values. */
    public static final String BLACKLISTED = "[ BLACKLISTED ]";

    private ResponseLogger() {
        // utility class
    }

    /**
     * Logs the selected fields of the given raw response at INFO level.
     *
     * @param raw       the raw HTTP response
     * @param fields    which parts of the response to log
     * @param blacklist case-insensitive set of header names whose values must be masked
     */
    public static void log(RawResponse raw, EnumSet<ResponseLogSpec.Field> fields, Set<String> blacklist) {
        LOG.info(format(raw, fields, blacklist));
    }

    /**
     * Logs the full response at ERROR level (used for {@code ifError} and
     * {@code ifValidationFails} variants).
     *
     * @param raw       the raw HTTP response
     * @param blacklist case-insensitive set of header names whose values must be masked
     */
    public static void logError(RawResponse raw, Set<String> blacklist) {
        LOG.error(format(raw, EnumSet.allOf(ResponseLogSpec.Field.class), blacklist));
    }

    private static String format(RawResponse raw, EnumSet<ResponseLogSpec.Field> fields, Set<String> blacklist) {
        StringBuilder sb = new StringBuilder();
        sb.append("Response\n");
        sb.append("--------\n");

        if (fields.contains(ResponseLogSpec.Field.STATUS)) {
            sb.append("Status:  ").append(raw.statusCode());
            if (raw.statusMessage() != null && !raw.statusMessage().isEmpty()) {
                sb.append(' ').append(raw.statusMessage());
            }
            sb.append('\n');
        }

        if (fields.contains(ResponseLogSpec.Field.HEADERS)) {
            sb.append("Headers:\n");
            for (Header h : raw.headers()) {
                String value = LogSupport.isBlacklisted(h.name(), blacklist) ? BLACKLISTED : h.value();
                sb.append("  ").append(h.name()).append(": ").append(value).append('\n');
            }
        }

        if (fields.contains(ResponseLogSpec.Field.BODY)) {
            byte[] body = raw.body();
            if (body != null && body.length > 0) {
                sb.append("Body:\n").append(new String(body, StandardCharsets.UTF_8)).append('\n');
            } else {
                sb.append("Body: <none>\n");
            }
        }

        return sb.toString().stripTrailing();
    }
}
