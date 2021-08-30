package ru.craftysoft.orderingsystem.gateway.provider;

import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.gateway.builder.operation.ErrorResponseDataBuilder;
import ru.craftysoft.orderingsystem.gateway.error.operation.ModuleOperationCode;
import ru.craftysoft.orderingsystem.util.error.exception.BaseException;
import ru.craftysoft.orderingsystem.util.error.exception.SecurityException;
import ru.craftysoft.orderingsystem.util.error.exception.ValidationException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static ru.craftysoft.orderingsystem.gateway.error.exception.SecurityExceptionCode.UNAUTHORIZED;
import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.mapException;
import static ru.craftysoft.orderingsystem.util.error.logging.ExceptionLoggerHelper.logError;

@Provider
@Singleton
@Slf4j
public class ExceptionHandler implements ExceptionMapper<Throwable> {

    private final ErrorResponseDataBuilder errorResponseDataBuilder;

    @Inject
    public ExceptionHandler(ErrorResponseDataBuilder errorResponseDataBuilder) {
        this.errorResponseDataBuilder = errorResponseDataBuilder;
    }

    @Override
    public Response toResponse(Throwable throwable) {
        var baseException = mapException(throwable, ModuleOperationCode::resolve);
        logError(log, "ExceptionHandler.toResponse", baseException);
        int status = resolveStatus(baseException);
        var errorResponse = errorResponseDataBuilder.build(baseException);
        return Response.status(status).entity(errorResponse).build();
    }

    private int resolveStatus(BaseException baseException) {
        if (baseException instanceof SecurityException) {
            return UNAUTHORIZED.getCode().equals(baseException.getFullErrorCode().split("-")[1])
                    ? 401
                    : 403;
        } else if (baseException instanceof ValidationException) {
            return 400;
        }
        return 500;
    }
}
