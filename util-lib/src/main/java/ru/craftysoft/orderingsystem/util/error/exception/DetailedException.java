package ru.craftysoft.orderingsystem.util.error.exception;

import ru.craftysoft.orderingsystem.util.error.code.ExceptionCode;
import ru.craftysoft.orderingsystem.util.error.operation.OperationCode;
import ru.craftysoft.orderingsystem.util.error.type.ExceptionType;

abstract class DetailedException extends BaseException {

    DetailedException(String message, Throwable cause, String service, OperationCode operation, ExceptionType type, ExceptionCode<? extends DetailedException> code, Object payload) {
        super(message, cause, service, operation, type, code, payload);
    }
}
