package ru.craftysoft.orderingsystem.util.error.code;

import ru.craftysoft.orderingsystem.util.error.exception.BaseException;

public interface ExceptionCode<T extends BaseException> {
    String OTHER = "99";

    String getCode();

    String getMessage();
}
