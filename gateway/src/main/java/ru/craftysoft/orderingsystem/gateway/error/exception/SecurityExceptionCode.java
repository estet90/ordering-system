package ru.craftysoft.orderingsystem.gateway.error.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.craftysoft.orderingsystem.util.error.code.ExceptionCode;
import ru.craftysoft.orderingsystem.util.error.exception.ValidationException;

@RequiredArgsConstructor
@Getter
public enum SecurityExceptionCode implements ExceptionCode<ValidationException> {

    UNAUTHORIZED("01", "Пользователь не авторизован"),
    FORBIDDEN("02", "Пользователь не имеет прав на выполнение операции"),
    ;

    private final String code;
    private final String message;
}
