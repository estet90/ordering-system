package ru.craftysoft.orderingsystem.util.error.exception;

import ru.craftysoft.orderingsystem.util.error.code.ExceptionCode;
import ru.craftysoft.orderingsystem.util.error.operation.OperationCode;

import static ru.craftysoft.orderingsystem.util.error.type.ExceptionType.BUSINESS;

public class BusinessException extends DetailedException {

    BusinessException(String message, Throwable cause, String service, OperationCode operation, ExceptionCode<BusinessException> code, Object payload) {
        super(message, cause, service, operation, BUSINESS, code, payload);
    }
}
