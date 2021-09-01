package ru.craftysoft.orderingsystem.orderprocessing.error.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.craftysoft.orderingsystem.util.error.code.ExceptionCode;
import ru.craftysoft.orderingsystem.util.error.exception.InvocationException;

@RequiredArgsConstructor
@Getter
public enum InvocationExceptionCode implements ExceptionCode<InvocationException> {

    DB("01", "Ошибка при запросе в БД"),
    CUSTOMER_SERVICE("02", "Ошибка при вызове customer-service"),
    EXECUTOR_SERVICE("03", "Ошибка при вызове executor-service"),
    REDIS_SEND("04", "Ошибка при отправке сообщения в Redis"),
    REDIS_SUBSCRIBE("05", "Ошибка получении сообщений из Redis"),
    ;

    private final String code;
    private final String message;

}
