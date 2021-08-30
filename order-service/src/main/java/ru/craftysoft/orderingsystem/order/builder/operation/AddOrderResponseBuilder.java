package ru.craftysoft.orderingsystem.order.builder.operation;

import ru.craftysoft.orderingsystem.order.proto.AddOrderResponse;
import ru.craftysoft.orderingsystem.order.proto.AddOrderResponseData;
import ru.craftysoft.orderingsystem.util.error.exception.BaseException;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AddOrderResponseBuilder {

    private final ErrorBuilder errorBuilder;

    @Inject
    public AddOrderResponseBuilder(ErrorBuilder errorBuilder) {
        this.errorBuilder = errorBuilder;
    }

    public AddOrderResponse build(BaseException baseException) {
        var error = errorBuilder.build(baseException);
        return AddOrderResponse.newBuilder()
                .setError(error)
                .build();
    }

    public AddOrderResponse build(Long id) {
        return AddOrderResponse.newBuilder()
                .setAddOrderResponseData(AddOrderResponseData.newBuilder().setId(id))
                .build();
    }
}
