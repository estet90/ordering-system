package ru.craftysoft.orderingsystem.util.error.exception;

import ru.craftysoft.orderingsystem.util.error.code.ExceptionCode;
import ru.craftysoft.orderingsystem.util.error.operation.OperationCode;
import ru.craftysoft.orderingsystem.util.error.type.ExceptionType;

import java.sql.SQLException;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;

public class ExceptionFactory {

    private static final String DEFAULT_MESSAGE = "Message not defined";

    private static String serviceCode;

    public ExceptionFactory(String serviceCode) {
        ExceptionFactory.serviceCode = serviceCode;
    }

    public static BaseException mapException(Throwable throwable, Supplier<OperationCode> operationCodeSupplier) {
        if (throwable instanceof BaseException baseException) {
            return baseException;
        } else if (throwable instanceof CompletionException completionException) {
            return ofNullable(completionException.getCause())
                    .map(cause -> mapException(cause, operationCodeSupplier))
                    .orElseGet(() -> newInternalException(completionException, operationCodeSupplier.get(), throwable.getMessage()));
        }
        return newInternalException(throwable, operationCodeSupplier.get(), throwable.getMessage());
    }

    public static BusinessException newBusinessException(OperationCode operationCode,
                                                         ExceptionCode<BusinessException> exceptionCode,
                                                         Object payload,
                                                         String... args) {
        String message = prepareMessage(exceptionCode, args);
        return new BusinessException(message, null, serviceCode, operationCode, exceptionCode, payload);
    }

    public static BusinessException newBusinessException(OperationCode operationCode,
                                                         ExceptionCode<BusinessException> exceptionCode,
                                                         String... args) {
        String message = prepareMessage(exceptionCode, args);
        return new BusinessException(message, null, serviceCode, operationCode, exceptionCode, null);
    }

    public static BusinessException newBusinessException(Throwable cause,
                                                         OperationCode operationCode,
                                                         ExceptionCode<BusinessException> exceptionCode,
                                                         String... args) {
        String message = prepareMessage(exceptionCode, args);
        return new BusinessException(message, cause, serviceCode, operationCode, exceptionCode, null);
    }

    public static InvocationException newInvocationException(OperationCode operation,
                                                             ExceptionCode<InvocationException> exceptionCode,
                                                             Object payload,
                                                             String... args) {
        String message = prepareMessage(exceptionCode, args);
        return new InvocationException(message, null, serviceCode, operation, exceptionCode, payload);
    }

    public static InvocationException newInvocationException(OperationCode operation,
                                                             ExceptionCode<InvocationException> exceptionCode,
                                                             String... args) {
        String message = prepareMessage(exceptionCode, args);
        return new InvocationException(message, null, serviceCode, operation, exceptionCode, null);
    }

    public static InvocationException newInvocationException(Throwable cause,
                                                             OperationCode operationCode,
                                                             ExceptionCode<InvocationException> exceptionCode,
                                                             String... args) {
        String message = prepareMessage(exceptionCode, args);
        return new InvocationException(message, cause, serviceCode, operationCode, exceptionCode, null);
    }

    public static InvocationException newInvocationException(RetryableException exception) {
        return new InvocationException(
                exception.getMessage(),
                exception.getCause(),
                exception.getService(),
                exception::getOperation,
                ExceptionType.INVOCATION,
                exception.getExceptionCode(),
                exception.getPayload());
    }

    public static RetryableException newRetryableException(OperationCode operation,
                                                           ExceptionCode<InvocationException> exceptionCode,
                                                           Object payload,
                                                           String... args) {
        String message = prepareMessage(exceptionCode, args);
        return new RetryableException(message, null, serviceCode, operation, exceptionCode, payload);
    }

    public static RetryableException newRetryableException(OperationCode operation,
                                                           ExceptionCode<InvocationException> exceptionCode,
                                                           String... args) {
        String message = prepareMessage(exceptionCode, args);
        return new RetryableException(message, null, serviceCode, operation, exceptionCode, null);
    }

    public static RetryableException newRetryableException(Throwable cause,
                                                           OperationCode operationCode,
                                                           ExceptionCode<InvocationException> exceptionCode,
                                                           String... args) {
        String message = prepareMessage(exceptionCode, args);
        return new RetryableException(message, cause, serviceCode, operationCode, exceptionCode, null);
    }

    public static ValidationException newValidationException(OperationCode operation,
                                                             ExceptionCode<ValidationException> exceptionCode,
                                                             Object payload,
                                                             String... args) {
        String message = prepareMessage(exceptionCode, args);
        return new ValidationException(message, null, serviceCode, operation, exceptionCode, payload);
    }

    public static ValidationException newValidationException(OperationCode operation,
                                                             ExceptionCode<ValidationException> exceptionCode,
                                                             String... args) {
        String message = prepareMessage(exceptionCode, args);
        return new ValidationException(message, null, serviceCode, operation, exceptionCode, null);
    }

    public static ValidationException newValidationException(Throwable cause,
                                                             OperationCode operationCode,
                                                             ExceptionCode<ValidationException> exceptionCode,
                                                             String... args) {
        String message = prepareMessage(exceptionCode, args);
        return new ValidationException(message, cause, serviceCode, operationCode, exceptionCode, null);
    }

    public static SecurityException newSecurityException(OperationCode operation,
                                                         ExceptionCode<ValidationException> exceptionCode,
                                                         Object payload,
                                                         String... args) {
        String message = prepareMessage(exceptionCode, args);
        return new SecurityException(message, null, serviceCode, operation, exceptionCode, payload);
    }

    public static SecurityException newSecurityException(OperationCode operation,
                                                         ExceptionCode<ValidationException> exceptionCode,
                                                         String... args) {
        String message = prepareMessage(exceptionCode, args);
        return new SecurityException(message, null, serviceCode, operation, exceptionCode, null);
    }

    public static SecurityException newSecurityException(Throwable cause,
                                                         OperationCode operationCode,
                                                         ExceptionCode<ValidationException> exceptionCode,
                                                         String... args) {
        String message = prepareMessage(exceptionCode, args);
        return new SecurityException(message, cause, serviceCode, operationCode, exceptionCode, null);
    }

    public static ValidationException newUnknownOperationException(Throwable cause) {
        return new ValidationException(cause, serviceCode);
    }

    public static InternalException newInternalException(Throwable cause,
                                                         OperationCode operationCode,
                                                         String... args) {
        String message = prepareInternalExceptionMessage(args);
        return new InternalException(message, cause, serviceCode, operationCode);
    }

    public static InvocationException mapSqlException(Throwable cause,
                                                      OperationCode operationCode,
                                                      ExceptionCode<InvocationException> exceptionCode) {
        return cause instanceof SQLException sqlException
                ? newInvocationException(sqlException, operationCode, exceptionCode, sqlException.getMessage())
                .setOriginalCode(String.valueOf(sqlException.getErrorCode()))
                .setOriginalMessage(sqlException.getSQLState())
                : newRetryableException(cause, operationCode, exceptionCode, cause.getMessage());
    }

    private static String prepareMessage(ExceptionCode<?> exceptionCode, String... args) {
        String message = exceptionCode.getMessage();
        return appendArguments(message != null ? message : DEFAULT_MESSAGE, args);
    }

    private static String prepareInternalExceptionMessage(String[] args) {
        return appendArguments("Other error", args);
    }

    private static String appendArguments(String message, String... args) {
        return message + stream(args).map(o -> ", '" + o + '\'').reduce(String::concat).orElse("");
    }
}
