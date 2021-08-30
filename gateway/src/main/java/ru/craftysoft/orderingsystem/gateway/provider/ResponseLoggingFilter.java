package ru.craftysoft.orderingsystem.gateway.provider;

import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.core.interception.jaxrs.SuspendableContainerResponseContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.util.Map;

import static java.util.Optional.ofNullable;

@Priority(Integer.MIN_VALUE)
@Provider
@Slf4j
@Singleton
public class ResponseLoggingFilter implements ContainerResponseFilter {

    private static final Logger responseLogger = LoggerFactory.getLogger("ru.craftysoft.orderingsystem.gateway.server.response");

    @Inject
    public ResponseLoggingFilter() {
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        var suspendableContainerResponseContext = (SuspendableContainerResponseContext) responseContext;
        suspendableContainerResponseContext.suspend();
        if (requestContext.getUriInfo().getPath().contains("/swagger")) {
            suspendableContainerResponseContext.resume();
            return;
        }
        if (responseLogger.isDebugEnabled()) {
            int status = responseContext.getStatus();
            var headers = responseContext.getHeaders();
            ofNullable(requestContext.getProperty("mdc"))
                    .map(mdc -> {
                        try {
                            return (Map<String, String>) mdc;
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .ifPresentOrElse(MDC::setContextMap, MDC::clear);
            try {
                responseLogger.debug("""
                                Status={}
                                Headers={}""",
                        status, headers);
            } finally {
                MDC.clear();
            }
        }
        suspendableContainerResponseContext.resume();
    }
}
