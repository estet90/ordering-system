package ru.craftysoft.orderingsystem.gateway.service.grpc;

import io.grpc.Metadata;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import ru.craftysoft.orderingsystem.user.proto.*;
import ru.craftysoft.orderingsystem.util.error.exception.InvocationException;
import ru.craftysoft.orderingsystem.util.grpc.ExceptionHelper;
import ru.craftysoft.orderingsystem.util.grpc.LoggingClientInterceptor;
import ru.craftysoft.orderingsystem.util.grpc.MetadataBuilder;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.util.Optional.ofNullable;
import static ru.craftysoft.orderingsystem.gateway.error.exception.InvocationExceptionCode.USER_SERVICE;
import static ru.craftysoft.orderingsystem.gateway.error.operation.ModuleOperationCode.resolve;
import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.newInvocationException;
import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.newRetryableException;
import static ru.craftysoft.orderingsystem.util.grpc.ExceptionHelper.buildKey;
import static ru.craftysoft.orderingsystem.util.grpc.ExceptionHelper.extractErrorResponse;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

@Singleton
@Slf4j
public class UserServiceClient {

    private final UserServiceGrpc.UserServiceStub userServiceStub;

    private final Metadata.Key<byte[]> getRolesResponseErrorKey = buildKey(GetRolesResponse.class);
    private final Metadata.Key<byte[]> getUserIdResponseErrorKey = buildKey(GetUserIdResponse.class);
    private final Function<Throwable, InvocationException> invocationExceptionFactory = throwable -> newInvocationException(throwable, resolve(), USER_SERVICE);
    private final Function<Throwable, InvocationException> retryableExceptionFactory = throwable -> newRetryableException(throwable, resolve(), USER_SERVICE);

    @Inject
    public UserServiceClient(UserServiceGrpc.UserServiceStub userServiceStub) {
        this.userServiceStub = userServiceStub;
    }

    public CompletableFuture<GetRolesResponse> getRoles(GetRolesRequest request) {
        var mdc = MDC.getCopyOfContextMap();
        var metadata = MetadataBuilder.build(mdc);
        var result = new CompletableFuture<GetRolesResponse>();
        userServiceStub
                .withInterceptors(new LoggingClientInterceptor(metadata))
                .getRoles(request, new StreamObserver<>() {
                    private GetRolesResponse response;
                    private final String point = "UserServiceClient.getRoles";

                    @Override
                    public void onNext(GetRolesResponse response) {
                        this.response = response;
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        withMdc(mdc, () -> log.error("{}.onError {}", point, throwable.getMessage()));
                        var baseException = ExceptionHelper.mapException(
                                log, point, mdc, throwable,
                                statusRuntimeException -> extractErrorResponse(statusRuntimeException, getRolesResponseErrorKey, GetRolesResponse::parseFrom),
                                invocationExceptionFactory,
                                retryableExceptionFactory,
                                (errorResponse, exception) -> ofNullable(errorResponse)
                                        .filter(GetRolesResponse::hasError)
                                        .map(GetRolesResponse::getError)
                                        .ifPresent(error -> exception
                                                .setOriginalCode(error.getCode())
                                                .setOriginalMessage(error.getMessage()))
                        );
                        result.completeExceptionally(baseException);
                    }

                    @Override
                    public void onCompleted() {
                        result.complete(this.response);
                    }
                });
        return result;
    }

    public CompletableFuture<GetUserIdResponse> getUserId(GetUserIdRequest request) {
        var mdc = MDC.getCopyOfContextMap();
        var metadata = MetadataBuilder.build(mdc);
        var result = new CompletableFuture<GetUserIdResponse>();
        userServiceStub
                .withInterceptors(new LoggingClientInterceptor(metadata))
                .getUserId(request, new StreamObserver<>() {
                    private GetUserIdResponse response;
                    private final String point = "UserServiceClient.getUserId";

                    @Override
                    public void onNext(GetUserIdResponse response) {
                        this.response = response;
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        withMdc(mdc, () -> log.error("{}.onError {}", point, throwable.getMessage()));
                        var baseException = ExceptionHelper.mapException(
                                log, point, mdc, throwable,
                                statusRuntimeException -> extractErrorResponse(statusRuntimeException, getUserIdResponseErrorKey, GetUserIdResponse::parseFrom),
                                invocationExceptionFactory,
                                retryableExceptionFactory,
                                (errorResponse, exception) -> ofNullable(errorResponse)
                                        .filter(GetUserIdResponse::hasError)
                                        .map(GetUserIdResponse::getError)
                                        .ifPresent(error -> exception
                                                .setOriginalCode(error.getCode())
                                                .setOriginalMessage(error.getMessage()))
                        );
                        result.completeExceptionally(baseException);
                    }

                    @Override
                    public void onCompleted() {
                        result.complete(this.response);
                    }
                });
        return result;
    }

}
