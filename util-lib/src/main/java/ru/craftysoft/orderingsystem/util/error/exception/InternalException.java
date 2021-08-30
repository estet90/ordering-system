package ru.craftysoft.orderingsystem.util.error.exception;

import ru.craftysoft.orderingsystem.util.error.operation.OperationCode;

import static ru.craftysoft.orderingsystem.util.error.type.ExceptionType.INTERNAL;

public class InternalException extends BaseException {

    InternalException(String message, Throwable cause, String service, OperationCode operation) {
        super(message, cause, service, operation, INTERNAL, OTHER_EXCEPTION_CODE, null);
    }
}
