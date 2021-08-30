package ru.craftysoft.orderingsystem.util.error.exception;

import ru.craftysoft.orderingsystem.util.error.code.ExceptionCode;
import ru.craftysoft.orderingsystem.util.error.operation.OperationCode;

import static ru.craftysoft.orderingsystem.util.error.type.ExceptionType.RETRYABLE;

public class RetryableException extends InvocationException {

    private final ExceptionCode<InvocationException> exceptionCode;

    RetryableException(String message, Throwable cause, String service, OperationCode operation, ExceptionCode<InvocationException> code, Object payload) {
        super(message, cause, service, operation, RETRYABLE, code, payload);
        this.exceptionCode = code;
    }

    public ExceptionCode<InvocationException> getExceptionCode() {
        return exceptionCode;
    }
}
