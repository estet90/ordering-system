package ru.craftysoft.orderingsystem.gateway.error.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.craftysoft.orderingsystem.util.error.code.ExceptionCode;
import ru.craftysoft.orderingsystem.util.error.exception.InvocationException;

@RequiredArgsConstructor
@Getter
public enum InvocationExceptionCode implements ExceptionCode<InvocationException> {

    CUSTOMER_SERVICE("01", "Ошибка при вызове customer-service"),
    EXECUTOR_SERVICE("02", "Ошибка при вызове executor-service"),
    ORDER_SERVICE("03", "Ошибка при вызове order-service"),
    USER_SERVICE("04", "Ошибка при вызове user-service"),
    ;

    private final String code;
    private final String message;

}
