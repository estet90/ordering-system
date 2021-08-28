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
                        throw new RuntimeException("Пользователь не имеет ролей");
                    }
                    for (var role : userRoles) {
                        if (roles.contains(role)) {
                            return;
                        }
                    }
                    throw new RuntimeException("Пользователь не имеет ни одной из требуемых ролей");
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
