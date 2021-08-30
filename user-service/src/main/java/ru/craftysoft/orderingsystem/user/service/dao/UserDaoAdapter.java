package ru.craftysoft.orderingsystem.user.service.dao;

import ru.craftysoft.orderingsystem.user.proto.GetRolesRequest;
import ru.craftysoft.orderingsystem.user.proto.GetUserIdRequest;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static java.util.Optional.ofNullable;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

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
        return CompletableFuture.supplyAsync(
                withMdc(() -> ofNullable(dao.getRoles(request.getUserLogin(), request.getUserPassword())).orElseGet(List::of)),
                dbExecutor
        );
    }

    public CompletableFuture<Long> getUserId(GetUserIdRequest request) {
        return CompletableFuture.supplyAsync(
                withMdc(() -> dao.getUserId(request.getLogin())),
                dbExecutor
        );
    }
}
