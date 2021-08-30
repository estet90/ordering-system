package ru.craftysoft.orderingsystem.util.error.exception;

import ru.craftysoft.orderingsystem.util.error.code.ExceptionCode;
import ru.craftysoft.orderingsystem.util.error.operation.OperationCode;
import ru.craftysoft.orderingsystem.util.error.type.ExceptionType;

public class InvocationException extends DetailedException {

    InvocationException(String message, Throwable cause, String service, OperationCode operation, ExceptionCode<InvocationException> code, Object payload) {
        this(message, cause, service, operation, ExceptionType.INVOCATION, code, payload);
    }

    InvocationException(String message, Throwable cause, String service, OperationCode operation, ExceptionType type, ExceptionCode<InvocationException> code, Object payload) {
        super(message, cause, service, operation, type, code, payload);
    }
}
