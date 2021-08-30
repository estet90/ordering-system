package ru.craftysoft.orderingsystem.util.error.exception;

import ru.craftysoft.orderingsystem.util.error.code.ExceptionCode;
import ru.craftysoft.orderingsystem.util.error.operation.OperationCode;

import static ru.craftysoft.orderingsystem.util.error.type.ExceptionType.SECURITY;

public class SecurityException extends ValidationException {

    SecurityException(String message, Throwable cause, String service, OperationCode operation, ExceptionCode<ValidationException> code, Object payload) {
        super(message, cause, service, operation, SECURITY, code, payload);
    }
}
