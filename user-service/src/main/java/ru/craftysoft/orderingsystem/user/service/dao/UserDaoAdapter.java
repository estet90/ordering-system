package ru.craftysoft.orderingsystem.user.service.dao;

import io.grpc.Context;
import ru.craftysoft.orderingsystem.user.proto.GetRolesRequest;
import ru.craftysoft.orderingsystem.user.proto.GetUserIdRequest;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withContext;

@Singleton
public class UserDaoAdapter {

    private final UserDao dao;
    private final Executor dbExecutor;

    @Inject
    public UserDaoAdapter(UserDao dao,
                          @Named("dbExecutor") Executor dbExecutor) {
        this.dao = dao;
        this.dbExecutor = dbExecutor;
    }

    public CompletableFuture<List<String>> getRoles(GetRolesRequest request) {
        var context = Context.current();
        return CompletableFuture.supplyAsync(
                () -> withContext(context, () -> dao.getRoles(request.getUserLogin(), request.getUserPassword())),
                dbExecutor
        );
    }

    public CompletableFuture<Long> getUserId(GetUserIdRequest request) {
        var context = Context.current();
        return CompletableFuture.supplyAsync(
                () -> withContext(context, () -> dao.getUserId(request.getLogin())),
                dbExecutor
        );
    }
}
