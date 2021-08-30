package ru.craftysoft.orderingsystem.util.error.code;

public final class OtherExceptionCode implements ExceptionCode {

    private static final String OTHER_EXCEPTION_MESSAGE = "Other exception";

    @Override
    public String getCode() {
        return OTHER;
    }

    @Override
    public String getMessage() {
        return OTHER_EXCEPTION_MESSAGE;
    }
}
