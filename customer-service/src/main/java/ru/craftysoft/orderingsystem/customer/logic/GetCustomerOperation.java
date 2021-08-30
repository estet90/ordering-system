package ru.craftysoft.orderingsystem.customer.logic;

import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.customer.builder.operation.GetCustomerResponseBuilder;
import ru.craftysoft.orderingsystem.customer.error.operation.ModuleOperationCode;
import ru.craftysoft.orderingsystem.customer.proto.GetCustomerRequest;
import ru.craftysoft.orderingsystem.customer.proto.GetCustomerResponse;
import ru.craftysoft.orderingsystem.customer.service.dao.CustomerDaoAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.mapException;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

@Singleton
@Slf4j
public class GetCustomerOperation {

    private final CustomerDaoAdapter customerDaoAdapter;
    private final GetCustomerResponseBuilder responseBuilder;

    @Inject
    public GetCustomerOperation(CustomerDaoAdapter customerDaoAdapter, GetCustomerResponseBuilder responseBuilder) {
        this.customerDaoAdapter = customerDaoAdapter;
        this.responseBuilder = responseBuilder;
    }

    public CompletableFuture<GetCustomerResponse> process(GetCustomerRequest request) {
        log.info("GetCustomerOperation.process.in");
        return customerDaoAdapter.getCustomer(request)
                .handleAsync(withMdc((customer, throwable) -> {
                    if (throwable != null) {
                        log.error("GetCustomerOperation.process.thrown {}", throwable.getMessage());
                        throw mapException(throwable, ModuleOperationCode::resolve);
                    }
                    log.error("GetCustomerOperation.process.out");
                    return responseBuilder.build(customer);
                }));
    }
}
