package ru.craftysoft.orderingsystem.util.error.exception;

import ru.craftysoft.orderingsystem.util.error.code.ExceptionCode;
import ru.craftysoft.orderingsystem.util.error.operation.OperationCode;
import ru.craftysoft.orderingsystem.util.error.type.ExceptionType;

import static ru.craftysoft.orderingsystem.util.error.type.ExceptionType.VALIDATION;

public class ValidationException extends DetailedException {

    ValidationException(Throwable cause, String service) {
        this(UNKNOWN_OPERATION_MESSAGE, cause, service, () -> OperationCode.OTHER, OTHER_EXCEPTION_CODE, null);
    }

    ValidationException(String message, Throwable cause, String service, OperationCode operation, ExceptionCode<ValidationException> code, Object payload) {
        this(message, cause, service, operation, VALIDATION, code, payload);
    }

    ValidationException(String message, Throwable cause, String service, OperationCode operation, ExceptionType type, ExceptionCode<ValidationException> code, Object payload) {
        super(message, cause, service, operation, type, code, payload);
    }
}
