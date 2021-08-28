package ru.craftysoft.orderingsystem.orderprocessing.service.grpc;

import ru.craftysoft.orderingsystem.executor.proto.UpdateExecutorBalanceRequest;
import ru.craftysoft.orderingsystem.executor.proto.UpdateExecutorBalanceResponse;
import ru.craftysoft.orderingsystem.orderprocessing.proto.DecreaseExecutorAmountRequest;
import ru.craftysoft.orderingsystem.orderprocessing.proto.IncrementExecutorAmountRequest;
import ru.craftysoft.orderingsystem.util.properties.PropertyResolver;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.CompletableFuture;

import static ru.craftysoft.orderingsystem.executor.proto.UpdateExecutorBalanceResponseData.Result.BALANCE_HAS_NOT_BEEN_CHANGED;
import static ru.craftysoft.orderingsystem.util.proto.ProtoUtils.bigDecimalToMoney;
import static ru.craftysoft.orderingsystem.util.proto.ProtoUtils.moneyToBigDecimal;

@Singleton
public class ExecutorServiceClientAdapter {

    private final ExecutorServiceClient client;
    private final BigDecimal executorFeePart;

    @Inject
    public ExecutorServiceClientAdapter(ExecutorServiceClient client, PropertyResolver propertyResolver) {
        this.client = client;
        var commission = propertyResolver.getIntProperty("commission.percent");
        this.executorFeePart = (BigDecimal.valueOf(100).subtract(BigDecimal.valueOf(commission)))
                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_DOWN);
    }

    public CompletableFuture<UpdateExecutorBalanceResponse> incrementAmount(IncrementExecutorAmountRequest incrementExecutorAmountRequest) {
        var fee = bigDecimalToMoney(moneyToBigDecimal(incrementExecutorAmountRequest.getAmount()).multiply(executorFeePart));
        var request = UpdateExecutorBalanceRequest.newBuilder()
                .setId(incrementExecutorAmountRequest.getCustomerId())
                .setIncrementAmount(fee)
                .build();
        return client.updateExecutorBalance(request)
                .whenComplete((updateExecutorBalanceResponse, throwable) -> {
                    if (throwable != null) {
                        throw new RuntimeException(throwable);
                    }
                    if (BALANCE_HAS_NOT_BEEN_CHANGED.equals(updateExecutorBalanceResponse.getUpdateExecutorBalanceResponseData().getResult())) {
                        throw new RuntimeException();
                    }
                });
    }

    public CompletableFuture<UpdateExecutorBalanceResponse> decreaseAmount(DecreaseExecutorAmountRequest decreaseExecutorAmountRequest) {
        var fee = bigDecimalToMoney(moneyToBigDecimal(decreaseExecutorAmountRequest.getAmount()).multiply(executorFeePart));
        var request = UpdateExecutorBalanceRequest.newBuilder()
                .setId(decreaseExecutorAmountRequest.getCustomerId())
                .setDecreaseAmount(fee)
                .build();
        return client.updateExecutorBalance(request)
                .whenComplete((updateExecutorBalanceResponse, throwable) -> {
                    if (throwable != null) {
                        throw new RuntimeException(throwable);
                    }
                    if (BALANCE_HAS_NOT_BEEN_CHANGED.equals(updateExecutorBalanceResponse.getUpdateExecutorBalanceResponseData().getResult())) {
                        throw new RuntimeException();
                    }
                });
    }
}
