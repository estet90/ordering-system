package ru.craftysoft.orderingsystem.gateway.service.grpc;

import ru.craftysoft.orderingsystem.gateway.util.PasswordEncoder;
import ru.craftysoft.orderingsystem.user.proto.GetRolesRequest;
import ru.craftysoft.orderingsystem.user.proto.GetUserIdRequest;
import ru.craftysoft.orderingsystem.user.proto.GetUserIdResponse;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static ru.craftysoft.orderingsystem.gateway.error.exception.SecurityExceptionCode.FORBIDDEN;
import static ru.craftysoft.orderingsystem.gateway.error.operation.ModuleOperationCode.resolve;
import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.newSecurityException;

@Singleton
public class UserServiceClientAdapter {

    private final UserServiceClient client;
    private final Base64.Decoder decoder;

    @Inject
    public UserServiceClientAdapter(UserServiceClient client) {
        this.client = client;
        this.decoder = Base64.getDecoder();
    }

    public CompletableFuture<Void> checkRoles(String login, String password, Set<String> roles) {
        var encodedPassword = PasswordEncoder.encode(password);
        var request = GetRolesRequest.newBuilder()
                .setUserLogin(login)
                .setUserPassword(encodedPassword)
                .build();
        return client.getRoles(request)
                .thenAccept(response -> {
                    var userRoles = response.getGetRolesResponseData().getRolesList();
                    if (userRoles.isEmpty()) {
                        throw newSecurityException(resolve(), FORBIDDEN, "Пользователь с логином '%s' не имеет ролей".formatted(login));
                    }
                    for (var role : userRoles) {
                        if (roles.contains(role)) {
                            return;
                        }
                    }
                    throw newSecurityException(resolve(), FORBIDDEN, "Пользователь с логином '%s' не имеет ни одной из требуемых ролей".formatted(login));
                });
    }

    public CompletableFuture<GetUserIdResponse> getUserId(String authorization) {
        var parts = new String(decoder.decode(authorization.substring("Basic ".length())), StandardCharsets.UTF_8).split(":");
        var login = parts[0];
        var request = GetUserIdRequest.newBuilder()
                .setLogin(login)
                .build();
        return client.getUserId(request);
    }
}
