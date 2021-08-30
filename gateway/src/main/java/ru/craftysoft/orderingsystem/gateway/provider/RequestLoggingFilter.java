package ru.craftysoft.orderingsystem.gateway.provider;

import org.jboss.resteasy.core.interception.jaxrs.SuspendableContainerRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.craftysoft.orderingsystem.util.uuid.UuidUtils;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.craftysoft.orderingsystem.util.mdc.MdcKey.SPAN_ID;
import static ru.craftysoft.orderingsystem.util.mdc.MdcKey.TRACE_ID;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

@Priority(Integer.MIN_VALUE)
@Provider
@Singleton
public class RequestLoggingFilter implements ContainerRequestFilter {

    private static final Logger requestLogger = LoggerFactory.getLogger("ru.craftysoft.orderingsystem.gateway.server.request");

    @Inject
    public RequestLoggingFilter() {
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        var mdc = Map.of(
                TRACE_ID, UuidUtils.generateDefaultUuid(),
                SPAN_ID, UuidUtils.generateDefaultUuid()
        );
        requestContext.setProperty("mdc", mdc);
        var suspendableContainerRequestContext = (SuspendableContainerRequestContext) requestContext;
        suspendableContainerRequestContext.suspend();
        if (requestContext.getUriInfo().getPath().contains("/swagger")) {
            suspendableContainerRequestContext.resume();
            return;
        }
        if (requestLogger.isDebugEnabled()) {
            String url = resolveUrl(suspendableContainerRequestContext);
            var headers = suspendableContainerRequestContext.getHeaders();
            withMdc(mdc, () -> requestLogger.debug("""
                            URL={}
                            Headers={}""",
                    url, headers));
        }
        suspendableContainerRequestContext.resume();
    }

    private String resolveUrl(SuspendableContainerRequestContext suspendableContainerRequestContext) {
        var url = suspendableContainerRequestContext.getUriInfo().getAbsolutePath().toString();
        var queryParameters = suspendableContainerRequestContext.getUriInfo().getQueryParameters().entrySet().stream()
                .map(entry -> {
                    if (entry.getValue().size() == 1) {
                        return entry.getKey() + "=" + entry.getValue().get(0);
                    }
                    return entry.getValue().stream()
                            .map(value -> entry.getKey() + "=" + value)
                            .collect(Collectors.joining("&"));
                })
                .collect(Collectors.joining("&"));
        if (!queryParameters.isEmpty()) {
            return url + "?" + queryParameters;
        }
        return url;
    }
}
