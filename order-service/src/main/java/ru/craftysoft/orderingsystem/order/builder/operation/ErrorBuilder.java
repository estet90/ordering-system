package ru.craftysoft.orderingsystem.order.builder.operation;

import ru.craftysoft.orderingsystem.order.proto.Error;
import ru.craftysoft.orderingsystem.util.error.exception.BaseException;

import javax.inject.Inject;
import javax.inject.Singleton;

import static ru.craftysoft.orderingsystem.util.proto.ProtoUtils.buildStringValue;

@Singleton
public class ErrorBuilder {

    @Inject
    public ErrorBuilder() {
    }

    public Error build(BaseException baseException) {
        return Error.newBuilder()
                .setCode(baseException.getFullErrorCode())
                .setMessage(baseException.getMessage())
                .setOriginalCode(buildStringValue(baseException.getOriginalCode()))
                .setOriginalMessage(buildStringValue(baseException.getOriginalMessage()))
                .build();
    }
}
