package ru.craftysoft.orderingsystem.orderprocessing.builder.grpc;

import ru.craftysoft.orderingsystem.executor.proto.UpdateExecutorBalanceRequest;
import ru.craftysoft.orderingsystem.orderprocessing.proto.DecreaseExecutorAmountRequest;
import ru.craftysoft.orderingsystem.orderprocessing.proto.IncrementExecutorAmountRequest;
import ru.craftysoft.orderingsystem.util.properties.PropertyResolver;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static ru.craftysoft.orderingsystem.util.proto.ProtoUtils.bigDecimalToMoney;
import static ru.craftysoft.orderingsystem.util.proto.ProtoUtils.moneyToBigDecimal;

@Singleton
public class UpdateExecutorBalanceResponse {

    private final BigDecimal executorFeePart;

    @Inject
    public UpdateExecutorBalanceResponse(PropertyResolver propertyResolver) {
        var commission = propertyResolver.getIntProperty("commission.percent");
        this.executorFeePart = (BigDecimal.valueOf(100).subtract(BigDecimal.valueOf(commission)))
                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_DOWN);
    }

    public UpdateExecutorBalanceRequest build(IncrementExecutorAmountRequest incrementExecutorAmountRequest) {
        var fee = bigDecimalToMoney(moneyToBigDecimal(incrementExecutorAmountRequest.getAmount()).multiply(executorFeePart));
        return UpdateExecutorBalanceRequest.newBuilder()
                .setId(incrementExecutorAmountRequest.getCustomerId())
                .setIncrementAmount(fee)
                .build();
    }

    public UpdateExecutorBalanceRequest build(DecreaseExecutorAmountRequest decreaseExecutorAmountRequest) {
        var fee = bigDecimalToMoney(moneyToBigDecimal(decreaseExecutorAmountRequest.getAmount()).multiply(executorFeePart));
        return UpdateExecutorBalanceRequest.newBuilder()
                .setId(decreaseExecutorAmountRequest.getCustomerId())
                .setDecreaseAmount(fee)
                .build();
    }

}
