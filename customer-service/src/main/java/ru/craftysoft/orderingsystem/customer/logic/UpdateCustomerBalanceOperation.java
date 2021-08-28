package ru.craftysoft.orderingsystem.customer.logic;

import com.google.type.Money;
import io.grpc.Context;
import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.customer.proto.UpdateCustomerBalanceRequest;
import ru.craftysoft.orderingsystem.customer.proto.UpdateCustomerBalanceResponse;
import ru.craftysoft.orderingsystem.customer.proto.UpdateCustomerBalanceResponseData;
import ru.craftysoft.orderingsystem.customer.service.dao.CustomerDaoAdapter;
import ru.craftysoft.orderingsystem.util.proto.ProtoUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

import static java.util.Optional.ofNullable;
import static ru.craftysoft.orderingsystem.customer.proto.UpdateCustomerBalanceResponseData.Result.BALANCE_HAS_BEEN_CHANGED;
import static ru.craftysoft.orderingsystem.customer.proto.UpdateCustomerBalanceResponseData.Result.BALANCE_HAS_NOT_BEEN_CHANGED;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withContext;

@Singleton
@Slf4j
public class UpdateCustomerBalanceOperation {

    private final CustomerDaoAdapter customerDaoAdapter;

    @Inject
    public UpdateCustomerBalanceOperation(CustomerDaoAdapter customerDaoAdapter) {
        this.customerDaoAdapter = customerDaoAdapter;
    }

    public CompletableFuture<UpdateCustomerBalanceResponse> process(UpdateCustomerBalanceRequest request) {
        log.info("UpdateCustomerBalanceOperation.process.in");
        var context = Context.current();
        return customerDaoAdapter.updateCustomerBalance(request)
                .handleAsync((balance, throwable) -> {
                    if (throwable != null) {
                        withContext(context, () -> log.error("UpdateCustomerBalanceOperation.process.thrown {}", throwable.getMessage()));
                        throw new RuntimeException(throwable);
                    }
                    withContext(context, () -> log.error("UpdateCustomerBalanceOperation.process.out"));
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
                });
    }
}
