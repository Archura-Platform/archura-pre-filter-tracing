package io.archura.platform.imperativeshell.pre.filter.tracing;

import io.archura.platform.api.attribute.TraceKeys;
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

    private static final String CONFIG_KEY_HEADERS = "headers";
    private Map<String, Object> configuration = new HashMap<>();

    @Override
    public void accept(HttpServerRequest request) {
        final Context context = (Context) request.getAttributes().get(Context.class.getSimpleName());
        final Logger logger = context.getLogger();
        final UUID uuid = UUID.randomUUID();
        final long currentTime = System.currentTimeMillis();

        final String requestSpanId = request.getFirstHeader(TraceKeys.SPAN_HEADER_NAME.getKey());
        if (isNull(requestSpanId)) {
            final String newSpanId = String.format("%s|%s", currentTime, uuid);
            request.getRequestHeaders().put(TraceKeys.SPAN_HEADER_NAME.getKey(), List.of(newSpanId));
        }
        final String currentSpanId = request.getFirstHeader(TraceKeys.SPAN_HEADER_NAME.getKey());

        final String traceId = String.format("%s|%s", currentTime, uuid);
        request.getRequestHeaders().put(TraceKeys.TRACE_HEADER_NAME.getKey(), List.of(traceId));

        request.getAttributes().put(TraceKeys.SPAN_HEADER_NAME.getKey(), requestSpanId);
        request.getAttributes().put(TraceKeys.TRACE_HEADER_NAME.getKey(), traceId);
        request.getAttributes().put(TraceKeys.SPAN_REQUEST_URL.getKey(), request.getRequestURI().toString());
        request.getAttributes().put(TraceKeys.SPAN_HTTP_METHOD.getKey(), request.getRequestMethod());
        if (configuration.containsKey(CONFIG_KEY_HEADERS)
                && nonNull(configuration.get(CONFIG_KEY_HEADERS))
                && configuration.get(CONFIG_KEY_HEADERS) instanceof List<?> headerNames) {
            final Map<String, List<String>> headerValueMap = new HashMap<>();
            for (Object headerNameObject : headerNames) {
                final String headerName = String.valueOf(headerNameObject);
                final List<String> headerValues = request.getRequestHeaders().get(headerName);
                if (nonNull(headerValues)) {
                    headerValueMap.put(headerName, headerValues);
                }
            }
            request.getAttributes().put(TraceKeys.SPAN_HEADERS.getKey(), headerValueMap);
        }

        logger.debug("Span header: '%s', value: '%s', Trace header: '%s', value: '%s'", TraceKeys.SPAN_HEADER_NAME.getKey(), currentSpanId, TraceKeys.TRACE_HEADER_NAME.getKey(), traceId);
    }

    @Override
    public void setConfiguration(Map<String, Object> configuration) {
        if (nonNull(configuration)) {
            this.configuration = configuration;
        }
    }

}
