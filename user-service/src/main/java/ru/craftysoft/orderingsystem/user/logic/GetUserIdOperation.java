package ru.craftysoft.orderingsystem.user.logic;

import io.grpc.Context;
import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.user.proto.GetUserIdRequest;
import ru.craftysoft.orderingsystem.user.proto.GetUserIdResponse;
import ru.craftysoft.orderingsystem.user.proto.GetUserIdResponseData;
import ru.craftysoft.orderingsystem.user.service.dao.UserDaoAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withContext;

@Singleton
@Slf4j
public class GetUserIdOperation {

    private final UserDaoAdapter userDaoAdapter;

    @Inject
    public GetUserIdOperation(UserDaoAdapter userDaoAdapter) {
        this.userDaoAdapter = userDaoAdapter;
    }

    public CompletableFuture<GetUserIdResponse> process(GetUserIdRequest request) {
        log.info("GetUserIdOperation.process.in");
        var context = Context.current();
        return userDaoAdapter.getUserId(request)
                .handleAsync((id, throwable) -> {
                    if (throwable != null) {
                        withContext(context, () -> log.error("GetUserIdOperation.process.thrown {}", throwable.getMessage()));
                        throw new RuntimeException(throwable);
                    }
                    withContext(context, () -> log.info("GetUserIdOperation.process.out"));
                    return GetUserIdResponse.newBuilder()
                            .setGetUserResponseData(GetUserIdResponseData.newBuilder().setId(id))
                            .build();
                });
    }
}
