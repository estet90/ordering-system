package ru.craftysoft.orderingsystem.customer.logic;

import io.grpc.Context;
import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.customer.proto.GetCustomerRequest;
import ru.craftysoft.orderingsystem.customer.proto.GetCustomerResponse;
import ru.craftysoft.orderingsystem.customer.proto.GetCustomerResponseData;
import ru.craftysoft.orderingsystem.customer.service.dao.CustomerDaoAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withContext;
import static ru.craftysoft.orderingsystem.util.proto.ProtoUtils.bigDecimalToMoney;

@Singleton
@Slf4j
public class GetCustomerOperation {

    private final CustomerDaoAdapter customerDaoAdapter;

    @Inject
    public GetCustomerOperation(CustomerDaoAdapter customerDaoAdapter) {
        this.customerDaoAdapter = customerDaoAdapter;
    }

    public CompletableFuture<GetCustomerResponse> process(GetCustomerRequest request) {
        log.info("AddOrderOperation.process.in");
        var context = Context.current();
        return customerDaoAdapter.getCustomer(request)
                .handleAsync((customer, throwable) -> {
                    if (throwable != null) {
                        withContext(context, () -> log.error("GetRolesOperation.process.thrown {}", throwable.getMessage()));
                        throw new RuntimeException(throwable);
                    }
                    withContext(context, () -> log.error("GetRolesOperation.process.out"));
                    return GetCustomerResponse.newBuilder()
                            .setGetCustomerResponseData(GetCustomerResponseData.newBuilder()
                                    .setId(customer.id())
                                    .setBalance(bigDecimalToMoney(customer.balance()))
                            )
                            .build();
                });
    }
}
