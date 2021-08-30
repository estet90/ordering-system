package ru.craftysoft.orderingsystem.gateway.provider;

import org.jboss.resteasy.core.interception.jaxrs.PostMatchContainerRequestContext;
import org.slf4j.MDC;
import ru.craftysoft.orderingsystem.gateway.error.operation.ModuleOperationCode;
import ru.craftysoft.orderingsystem.gateway.service.grpc.UserServiceClientAdapter;

import javax.annotation.Priority;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

import static java.util.Optional.ofNullable;
import static javax.ws.rs.Priorities.AUTHENTICATION;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static ru.craftysoft.orderingsystem.gateway.error.exception.SecurityExceptionCode.UNAUTHORIZED;
import static ru.craftysoft.orderingsystem.gateway.error.operation.ModuleOperationCode.resolve;
import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.mapException;
import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.newSecurityException;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

@Priority(AUTHENTICATION)
@Singleton
@Provider
public class SecurityRequestFilter implements ContainerRequestFilter {

    private final UserServiceClientAdapter userServiceClientAdapter;
    private final Base64.Decoder decoder;

    @Inject
    public SecurityRequestFilter(UserServiceClientAdapter userServiceClientAdapter) {
        this.userServiceClientAdapter = userServiceClientAdapter;
        this.decoder = Base64.getDecoder();
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        var postMatchContainerRequestContext = (PostMatchContainerRequestContext) requestContext;
        postMatchContainerRequestContext.suspend();
        var method = postMatchContainerRequestContext.getResourceMethod().getMethod();
        if (method.isAnnotationPresent(RolesAllowed.class)) {
            var authorization = requestContext.getHeaderString(AUTHORIZATION);
            if (authorization == null || !authorization.startsWith("Basic ")) {
                postMatchContainerRequestContext.resume(newSecurityException(resolve(), UNAUTHORIZED));
                return;
            }
            var parts = new String(decoder.decode(authorization.substring("Basic ".length())), StandardCharsets.UTF_8).split(":");
            var username = parts[0];
            var password = parts[1];
            var rolesAllowedAnnotation = method.getAnnotation(RolesAllowed.class);
            var rolesAllowed = Set.of(rolesAllowedAnnotation.value());
            try {
                ofNullable(requestContext.getProperty("mdc"))
                        .map(mdc -> {
                            try {
                                return (Map<String, String>) mdc;
                            } catch (Exception e) {
                                return null;
                            }
                        })
                        .ifPresentOrElse(MDC::setContextMap, MDC::clear);
                userServiceClientAdapter.checkRoles(username, password, rolesAllowed)
                        .whenComplete(withMdc((v, throwable) -> {
                            if (throwable != null) {
                                var baseException = mapException(throwable, ModuleOperationCode::resolve);
                                postMatchContainerRequestContext.resume(baseException);
                            } else {
                                postMatchContainerRequestContext.resume();
                            }
                        }));
            } finally {
                MDC.clear();
            }
        }
    }
}
