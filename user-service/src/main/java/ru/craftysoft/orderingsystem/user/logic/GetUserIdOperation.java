package ru.craftysoft.orderingsystem.user.logic;

import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.user.builder.operation.GetUserIdResponseBuilder;
import ru.craftysoft.orderingsystem.user.error.operation.ModuleOperationCode;
import ru.craftysoft.orderingsystem.user.proto.GetUserIdRequest;
import ru.craftysoft.orderingsystem.user.proto.GetUserIdResponse;
import ru.craftysoft.orderingsystem.user.service.dao.UserDaoAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.mapException;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

@Singleton
@Slf4j
public class GetUserIdOperation {

    private final UserDaoAdapter userDaoAdapter;
    private final GetUserIdResponseBuilder responseBuilder;

    @Inject
    public GetUserIdOperation(UserDaoAdapter userDaoAdapter, GetUserIdResponseBuilder responseBuilder) {
        this.userDaoAdapter = userDaoAdapter;
        this.responseBuilder = responseBuilder;
    }

    public CompletableFuture<GetUserIdResponse> process(GetUserIdRequest request) {
        log.info("GetUserIdOperation.process.in");
        return userDaoAdapter.getUserId(request)
                .handleAsync(withMdc((id, throwable) -> {
                    if (throwable != null) {
                        log.error("GetUserIdOperation.process.thrown {}", throwable.getMessage());
                        throw mapException(throwable, ModuleOperationCode::resolve);
                    }
                    log.info("GetUserIdOperation.process.out");
                    return responseBuilder.build(id);
                }));
    }
}
