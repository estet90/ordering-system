package ru.craftysoft.orderingsystem.customer.builder.operation;

import ru.craftysoft.orderingsystem.customer.dto.Customer;
import ru.craftysoft.orderingsystem.customer.proto.GetCustomerResponse;
import ru.craftysoft.orderingsystem.customer.proto.GetCustomerResponseData;
import ru.craftysoft.orderingsystem.util.error.exception.BaseException;

import javax.inject.Inject;
import javax.inject.Singleton;

import static ru.craftysoft.orderingsystem.util.proto.ProtoUtils.bigDecimalToMoney;

@Singleton
public class GetCustomerResponseBuilder {

    private final ErrorBuilder errorBuilder;

    @Inject
    public GetCustomerResponseBuilder(ErrorBuilder errorBuilder) {
        this.errorBuilder = errorBuilder;
    }

    public GetCustomerResponse build(BaseException baseException) {
        return GetCustomerResponse.newBuilder()
                .setError(errorBuilder.build(baseException))
                .build();
    }

    public GetCustomerResponse build(Customer customer) {
        return GetCustomerResponse.newBuilder()
                .setGetCustomerResponseData(GetCustomerResponseData.newBuilder()
                        .setId(customer.id())
                        .setBalance(bigDecimalToMoney(customer.balance()))
                )
                .build();
    }
}
