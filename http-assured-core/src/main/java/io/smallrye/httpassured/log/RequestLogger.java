package io.smallrye.httpassured.log;

import io.smallrye.httpassured.http.Header;
import io.smallrye.httpassured.spi.RequestContext;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Set;

/**
 * Stateless utility that formats and emits request log lines via JBoss Logging.
 * <p>
 * All output is at {@code INFO} level under the {@code io.smallrye.httpassured.log}
 * category. Header values whose names appear in the blacklist are replaced with
 * {@code [ BLACKLISTED ]}.
 * </p>
 */
public final class RequestLogger {

    private static volatile Logger LOG = Logger.getLogger("io.smallrye.httpassured.log");

    /**
     * Replaces the logger used by this class.
     *
     * @param logger the replacement logger (must not be {@code null})
     */
    static void setLogger(Logger logger) {
        LOG = logger;
    }

    /** Replacement text used for blacklisted header values. */
    public static final String BLACKLISTED = "[ BLACKLISTED ]";

    private RequestLogger() {
        // utility class
    }

    /**
     * Logs the selected fields of the given request context.
     *
     * @param ctx       the fully-built request context (after path/query param resolution)
     * @param fields    which parts of the request to log
     * @param blacklist case-insensitive set of header names whose values must be masked
     */
    public static void log(RequestContext ctx, EnumSet<RequestLogSpec.Field> fields, Set<String> blacklist) {
        if (fields.isEmpty()) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Request\n");
        sb.append("-------\n");

        if (fields.contains(RequestLogSpec.Field.METHOD) || fields.contains(RequestLogSpec.Field.URI)) {
            if (fields.contains(RequestLogSpec.Field.METHOD)) {
                sb.append("Method:    ").append(ctx.method()).append('\n');
            }
            if (fields.contains(RequestLogSpec.Field.URI)) {
                sb.append("URI:       ").append(ctx.uri()).append('\n');
            }
        }

        if (fields.contains(RequestLogSpec.Field.PARAMS)) {
            if (!ctx.queryParams().isEmpty()) {
                sb.append("Query params:\n");
                ctx.queryParams().forEach((k, values) -> {
                    for (String v : values) {
                        sb.append("  ").append(k).append('=').append(v).append('\n');
                    }
                });
            }
            if (!ctx.pathParams().isEmpty()) {
                sb.append("Path params:\n");
                ctx.pathParams().forEach((k, v) -> sb.append("  ").append(k).append('=').append(v).append('\n'));
            }
        }

        if (fields.contains(RequestLogSpec.Field.HEADERS)) {
            sb.append("Headers:\n");
            for (Header h : ctx.headers()) {
                String value = LogSupport.isBlacklisted(h.name(), blacklist) ? BLACKLISTED : h.value();
                sb.append("  ").append(h.name()).append(": ").append(value).append('\n');
            }
        }

        if (fields.contains(RequestLogSpec.Field.BODY)) {
            ctx.body().ifPresentOrElse(
                    body -> sb.append("Body:\n").append(new String(body, StandardCharsets.UTF_8)).append('\n'),
                    () -> sb.append("Body: <none>\n")
            );
        }

        LOG.info(sb.toString().stripTrailing());
    }
}
