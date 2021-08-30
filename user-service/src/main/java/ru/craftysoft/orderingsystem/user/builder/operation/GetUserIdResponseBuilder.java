package ru.craftysoft.orderingsystem.user.builder.operation;

import ru.craftysoft.orderingsystem.user.proto.GetUserIdResponse;
import ru.craftysoft.orderingsystem.user.proto.GetUserIdResponseData;
import ru.craftysoft.orderingsystem.util.error.exception.BaseException;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GetUserIdResponseBuilder {

    private final ErrorBuilder errorBuilder;

    @Inject
    public GetUserIdResponseBuilder(ErrorBuilder errorBuilder) {
        this.errorBuilder = errorBuilder;
    }

    public GetUserIdResponse build(BaseException baseException) {
        var error = errorBuilder.build(baseException);
        return GetUserIdResponse.newBuilder()
                .setError(error)
                .build();
    }

    public GetUserIdResponse build(Long id) {
        return GetUserIdResponse.newBuilder()
                .setGetUserResponseData(GetUserIdResponseData.newBuilder().setId(id))
                .build();
    }
}
