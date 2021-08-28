package ru.craftysoft.orderingsystem.orderprocessing.error.exception;

public class RetryExpiryException extends RuntimeException {

    public RetryExpiryException(Throwable cause) {
        super(cause);
    }

}
