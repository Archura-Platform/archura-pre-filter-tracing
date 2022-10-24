package io.archura.platform.imperativeshell.pre.filter.tracing;

import io.archura.platform.api.context.Context;
import io.archura.platform.api.http.HttpServerRequest;
import io.archura.platform.api.logger.Logger;
import io.archura.platform.api.type.Configurable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class TracingFilter implements Consumer<HttpServerRequest>, Configurable {

    public static final String TRACE_HEADER_NAME = "X-A-Trace-ID";
    public static final String SPAN_HEADER_NAME = "X-A-Span-ID";
    private Map<String, Object> configuration = new HashMap<>();

    @Override
    public void accept(HttpServerRequest request) {
        final Context context = (Context) request.getAttributes().get(Context.class.getSimpleName());
        final Logger logger = context.getLogger();
        final UUID uuid = UUID.randomUUID();
        final long currentTime = System.currentTimeMillis();

        final String spanHeaderName = String.valueOf(configuration.getOrDefault("SpanHeaderName", SPAN_HEADER_NAME));
        final String requestSpanId = request.getFirstHeader(spanHeaderName);
        if (isNull(requestSpanId)) {
            final String newSpanId = String.format("%s|%s", currentTime, uuid);
            request.getRequestHeaders().put(spanHeaderName, List.of(newSpanId));
        }
        final String currentSpanId = request.getFirstHeader(spanHeaderName);

        final String traceHeaderName = String.valueOf(configuration.getOrDefault("TraceHeaderName", TRACE_HEADER_NAME));
        final String traceId = String.format("%s|%s", currentTime, uuid);
        request.getRequestHeaders().put(traceHeaderName, List.of(traceId));

        logger.debug("Span header: '%s', value: '%s', Trace header: '%s', value: '%s'", spanHeaderName, currentSpanId, traceHeaderName, traceId);
    }

    @Override
    public void setConfiguration(Map<String, Object> configuration) {
        if (nonNull(configuration)) {
            this.configuration = configuration;
        }
    }

}
