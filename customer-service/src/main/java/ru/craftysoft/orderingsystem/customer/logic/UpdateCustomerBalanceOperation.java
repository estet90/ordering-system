package ru.craftysoft.orderingsystem.customer.logic;

import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.customer.builder.operation.UpdateCustomerBalanceResponseBuilder;
import ru.craftysoft.orderingsystem.customer.error.operation.ModuleOperationCode;
import ru.craftysoft.orderingsystem.customer.proto.UpdateCustomerBalanceRequest;
import ru.craftysoft.orderingsystem.customer.proto.UpdateCustomerBalanceResponse;
import ru.craftysoft.orderingsystem.customer.service.dao.CustomerDaoAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.mapException;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

@Singleton
@Slf4j
public class UpdateCustomerBalanceOperation {

    private final CustomerDaoAdapter customerDaoAdapter;
    private final UpdateCustomerBalanceResponseBuilder responseBuilder;

    @Inject
    public UpdateCustomerBalanceOperation(CustomerDaoAdapter customerDaoAdapter, UpdateCustomerBalanceResponseBuilder responseBuilder) {
        this.customerDaoAdapter = customerDaoAdapter;
        this.responseBuilder = responseBuilder;
    }

    public CompletableFuture<UpdateCustomerBalanceResponse> process(UpdateCustomerBalanceRequest request) {
        log.info("UpdateCustomerBalanceOperation.process.in");
        return customerDaoAdapter.updateCustomerBalance(request)
                .handleAsync(withMdc((balance, throwable) -> {
                    if (throwable != null) {
                        log.error("UpdateCustomerBalanceOperation.process.thrown {}", throwable.getMessage());
                        throw mapException(throwable, ModuleOperationCode::resolve);
                    }
                    log.error("UpdateCustomerBalanceOperation.process.out");
                    return responseBuilder.build(balance);
                }));
    }
}
