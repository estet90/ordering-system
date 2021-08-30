package ru.craftysoft.orderingsystem.customer.builder.operation;

import com.google.type.Money;
import ru.craftysoft.orderingsystem.customer.proto.UpdateCustomerBalanceResponse;
import ru.craftysoft.orderingsystem.customer.proto.UpdateCustomerBalanceResponseData;
import ru.craftysoft.orderingsystem.util.error.exception.BaseException;
import ru.craftysoft.orderingsystem.util.proto.ProtoUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;

import static java.util.Optional.ofNullable;
import static ru.craftysoft.orderingsystem.customer.proto.UpdateCustomerBalanceResponseData.Result.BALANCE_HAS_BEEN_CHANGED;
import static ru.craftysoft.orderingsystem.customer.proto.UpdateCustomerBalanceResponseData.Result.BALANCE_HAS_NOT_BEEN_CHANGED;

@Singleton
public class UpdateCustomerBalanceResponseBuilder {

    private final ErrorBuilder errorBuilder;

    @Inject
    public UpdateCustomerBalanceResponseBuilder(ErrorBuilder errorBuilder) {
        this.errorBuilder = errorBuilder;
    }

    public UpdateCustomerBalanceResponse build(BaseException baseException) {
        return UpdateCustomerBalanceResponse.newBuilder()
                .setError(errorBuilder.build(baseException))
                .build();
    }

    public UpdateCustomerBalanceResponse build(BigDecimal balance) {
        var result = balance != null
                ? BALANCE_HAS_BEEN_CHANGED
                : BALANCE_HAS_NOT_BEEN_CHANGED;
        var balanceAsMoney = ofNullable(balance)
                .map(ProtoUtils::bigDecimalToMoney)
                .orElseGet(Money::getDefaultInstance);
        return UpdateCustomerBalanceResponse.newBuilder()
                .setUpdateCustomerBalanceResponseData(UpdateCustomerBalanceResponseData.newBuilder()
                        .setBalance(balanceAsMoney)
                        .setResult(result)
                )
                .build();
    }
}
