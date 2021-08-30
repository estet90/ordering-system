package ru.craftysoft.orderingsystem.util.error.exception;

import ru.craftysoft.orderingsystem.util.error.code.ExceptionCode;
import ru.craftysoft.orderingsystem.util.error.code.OtherExceptionCode;
import ru.craftysoft.orderingsystem.util.error.operation.OperationCode;
import ru.craftysoft.orderingsystem.util.error.type.ExceptionType;

import static java.lang.String.format;
import static java.util.Objects.isNull;

public abstract class BaseException extends RuntimeException {

    static final String UNKNOWN_OPERATION_MESSAGE = "Unknown operation";
    static final ExceptionCode OTHER_EXCEPTION_CODE = new OtherExceptionCode();

    private final String service;
    private final String operation;
    private final String type;
    private final String code;
    private Object payload;
    private String originalCode;
    private String originalMessage;

    BaseException(String message,
                  Throwable cause,
                  String service,
                  OperationCode operation,
                  ExceptionType type,
                  ExceptionCode<?> code,
                  Object payload) {
        super(message, cause);
        this.service = service;
        this.operation = isNull(operation) ? null : operation.getCode();
        this.type = isNull(type) ? null : type.getCode();
        this.code = isNull(code) ? null : code.getCode();
        this.payload = payload;
    }

    public String getService() {
        return service;
    }

    public String getOperation() {
        return operation;
    }

    public String getType() {
        return type;
    }

    public String getCode() {
        return code;
    }

    public String getFullErrorCode() {
        return format("%s-%s-%s%s", service, operation, type, code);
    }

    public Object getPayload() {
        return payload;
    }

    public <T extends BaseException> T setPayload(Object payload) {
        this.payload = payload;
        return (T) this;
    }

    public String getOriginalCode() {
        return originalCode;
    }

    public <T extends BaseException> T setOriginalCode(String originalCode) {
        this.originalCode = originalCode;
        return (T) this;
    }

    public String getOriginalMessage() {
        return originalMessage;
    }

    public <T extends BaseException> T setOriginalMessage(String originalMessage) {
        this.originalMessage = originalMessage;
        return (T) this;
    }

    @Override
    public String toString() {
        return getFullErrorCode();
    }
}
