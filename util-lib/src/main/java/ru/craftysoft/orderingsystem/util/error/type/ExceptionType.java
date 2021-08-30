package ru.craftysoft.orderingsystem.util.error.type;

public enum ExceptionType {
    VALIDATION("V"),
    BUSINESS("B"),
    INVOCATION("I"),
    RETRYABLE("R"),
    SECURITY("S"),
    INTERNAL("Z");

    private final String code;

    ExceptionType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
