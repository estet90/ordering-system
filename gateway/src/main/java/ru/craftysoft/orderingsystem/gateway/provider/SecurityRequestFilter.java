package ru.craftysoft.orderingsystem.gateway.provider;

import org.jboss.resteasy.core.interception.jaxrs.PostMatchContainerRequestContext;
import ru.craftysoft.orderingsystem.gateway.service.grpc.UserServiceClientAdapter;

import javax.annotation.Priority;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

import static javax.ws.rs.Priorities.AUTHENTICATION;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;

@Priority(AUTHENTICATION)
@Singleton
@Provider
public class SecurityRequestFilter implements ContainerRequestFilter {

    private final UserServiceClientAdapter userServiceClientAdapter;

    @Inject
    public SecurityRequestFilter(UserServiceClientAdapter userServiceClientAdapter) {
        this.userServiceClientAdapter = userServiceClientAdapter;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        var postMatchContainerRequestContext = (PostMatchContainerRequestContext) requestContext;
        var method = postMatchContainerRequestContext.getResourceMethod().getMethod();
        if (method.isAnnotationPresent(RolesAllowed.class)) {
            var authorization = requestContext.getHeaderString(AUTHORIZATION);
            if (authorization == null || !authorization.startsWith("Basic ")) {
                var errorResponse = Response.status(Response.Status.UNAUTHORIZED)
                        .entity("[\"Unauthorized\"]")
                        .build();
                requestContext.abortWith(errorResponse);
                return;
            }
            var parts = (new String(Base64.getDecoder().decode(authorization.substring("Basic ".length())), StandardCharsets.UTF_8)).split(":");
            var username = parts[0];
            var password = parts[1];
            var rolesAllowedAnnotation = method.getAnnotation(RolesAllowed.class);
            var rolesAllowed = Set.of(rolesAllowedAnnotation.value());
            userServiceClientAdapter.checkRoles(username, password, rolesAllowed)
                    .whenComplete((v, throwable) -> {
                        if (throwable != null) {
                            var errorResponse = Response.status(Response.Status.FORBIDDEN)
                                    .entity("[\"Forbidden\"]")
                                    .build();
                            requestContext.abortWith(errorResponse);
                        }
                    });
        }
    }
}
