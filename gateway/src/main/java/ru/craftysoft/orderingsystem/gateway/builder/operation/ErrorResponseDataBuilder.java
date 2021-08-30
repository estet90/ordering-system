package ru.craftysoft.orderingsystem.gateway.builder.operation;

import ru.craftysoft.orderingsystem.gateway.order.rest.model.ErrorResponseData;
import ru.craftysoft.orderingsystem.util.error.exception.BaseException;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ErrorResponseDataBuilder {

    @Inject
    public ErrorResponseDataBuilder() {
    }

    public ErrorResponseData build(BaseException baseException) {
        var errorResponse = new ErrorResponseData();
        errorResponse.setCode(baseException.getFullErrorCode());
        errorResponse.setMessage(baseException.getMessage());
        errorResponse.setOriginalCode(baseException.getOriginalCode());
        errorResponse.setOriginalMessage(baseException.getOriginalMessage());
        return errorResponse;
    }
}
