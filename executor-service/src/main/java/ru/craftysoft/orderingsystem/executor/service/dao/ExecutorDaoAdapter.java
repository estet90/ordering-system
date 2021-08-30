package ru.craftysoft.orderingsystem.executor.service.dao;

import ru.craftysoft.orderingsystem.executor.proto.GetExecutorRequest;
import ru.craftysoft.orderingsystem.executor.proto.UpdateExecutorBalanceRequest;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;
import static ru.craftysoft.orderingsystem.util.proto.ProtoUtils.moneyToBigDecimal;

@Singleton
public class ExecutorDaoAdapter {

    private final ExecutorDao dao;
    private final Executor dbExecutor;

    @Inject
    public ExecutorDaoAdapter(ExecutorDao dao,
                              @Named("dbExecutor") Executor dbExecutor) {
        this.dao = dao;
        this.dbExecutor = dbExecutor;
    }

    public CompletableFuture<ru.craftysoft.orderingsystem.executor.dto.Executor> getExecutor(GetExecutorRequest request) {
        Supplier<ru.craftysoft.orderingsystem.executor.dto.Executor> callback = request.hasId()
                ? () -> dao.getExecutorById(request.getId())
                : () -> dao.getExecutorByUserId(request.getUserId());
        return CompletableFuture.supplyAsync(
                withMdc(callback),
                dbExecutor
        );
    }

    public CompletableFuture<Integer> updateExecutorBalance(UpdateExecutorBalanceRequest request) {
        Supplier<Integer> callback = request.hasIncrementAmount()
                ? () -> dao.incrementAmount(request.getId(), moneyToBigDecimal(request.getIncrementAmount()))
                : () -> dao.decreaseAmount(request.getId(), moneyToBigDecimal(request.getDecreaseAmount()));
        return CompletableFuture.supplyAsync(
                withMdc(callback),
                dbExecutor
        );
    }
}
