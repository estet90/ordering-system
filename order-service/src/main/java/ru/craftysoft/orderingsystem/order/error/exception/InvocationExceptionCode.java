package ru.craftysoft.orderingsystem.order.error.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.craftysoft.orderingsystem.util.error.code.ExceptionCode;
import ru.craftysoft.orderingsystem.util.error.exception.InvocationException;

@RequiredArgsConstructor
@Getter
public enum InvocationExceptionCode implements ExceptionCode<InvocationException> {

    DB("01", "Ошибка при запросе к БД"),
    ;

    private final String code;
    private final String message;

}
