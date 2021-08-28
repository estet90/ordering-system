package ru.craftysoft.orderingsystem.user.logic;

import io.grpc.Context;
import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.user.proto.GetRolesRequest;
import ru.craftysoft.orderingsystem.user.proto.GetRolesResponse;
import ru.craftysoft.orderingsystem.user.proto.GetRolesResponseData;
import ru.craftysoft.orderingsystem.user.service.dao.UserDaoAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withContext;

@Singleton
@Slf4j
public class GetRolesOperation {

    private final UserDaoAdapter roleDaoAdapter;

    @Inject
    public GetRolesOperation(UserDaoAdapter roleDaoAdapter) {
        this.roleDaoAdapter = roleDaoAdapter;
    }

    public CompletableFuture<GetRolesResponse> process(GetRolesRequest request) {
        log.info("GetRolesOperation.process.in");
        var context = Context.current();
        return roleDaoAdapter.getRoles(request)
                .handleAsync((roles, throwable) -> {
                    if (throwable != null) {
                        withContext(context, () -> log.error("GetRolesOperation.process.thrown {}", throwable.getMessage()));
                        throw new RuntimeException(throwable);
                    }
                    withContext(context, () -> log.info("GetRolesOperation.process.out"));
                    return GetRolesResponse.newBuilder()
                            .setGetRolesResponseData(GetRolesResponseData.newBuilder()
                                    .addAllRoles(roles)
                            ).build();
                });
    }

}
