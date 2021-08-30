package ru.craftysoft.orderingsystem.user.logic;

import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.user.builder.operation.GetRolesResponseBuilder;
import ru.craftysoft.orderingsystem.user.error.operation.ModuleOperationCode;
import ru.craftysoft.orderingsystem.user.proto.GetRolesRequest;
import ru.craftysoft.orderingsystem.user.proto.GetRolesResponse;
import ru.craftysoft.orderingsystem.user.service.dao.UserDaoAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.mapException;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

@Singleton
@Slf4j
public class GetRolesOperation {

    private final UserDaoAdapter roleDaoAdapter;
    private final GetRolesResponseBuilder responseBuilder;

    @Inject
    public GetRolesOperation(UserDaoAdapter roleDaoAdapter, GetRolesResponseBuilder responseBuilder) {
        this.roleDaoAdapter = roleDaoAdapter;
        this.responseBuilder = responseBuilder;
    }

    public CompletableFuture<GetRolesResponse> process(GetRolesRequest request) {
        log.info("GetRolesOperation.process.in");
        return roleDaoAdapter.getRoles(request)
                .handleAsync(withMdc((roles, throwable) -> {
                    if (throwable != null) {
                        log.error("GetRolesOperation.process.thrown {}", throwable.getMessage());
                        throw mapException(throwable, ModuleOperationCode::resolve);
                    }
                    log.info("GetRolesOperation.process.out");
                    return responseBuilder.build(roles);
                }));
    }

}
