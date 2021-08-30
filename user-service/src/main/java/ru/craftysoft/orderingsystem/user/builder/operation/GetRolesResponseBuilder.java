package ru.craftysoft.orderingsystem.user.builder.operation;

import ru.craftysoft.orderingsystem.user.proto.GetRolesResponse;
import ru.craftysoft.orderingsystem.user.proto.GetRolesResponseData;
import ru.craftysoft.orderingsystem.util.error.exception.BaseException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class GetRolesResponseBuilder {

    private final ErrorBuilder errorBuilder;

    @Inject
    public GetRolesResponseBuilder(ErrorBuilder errorBuilder) {
        this.errorBuilder = errorBuilder;
    }

    public GetRolesResponse build(BaseException baseException) {
        var error = errorBuilder.build(baseException);
        return GetRolesResponse.newBuilder()
                .setError(error)
                .build();
    }

    public GetRolesResponse build(List<String> roles) {
        return GetRolesResponse.newBuilder()
                .setGetRolesResponseData(GetRolesResponseData.newBuilder()
                        .addAllRoles(roles)
                        .build()
                )
                .build();
    }
}
