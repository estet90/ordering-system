package ru.craftysoft.orderingsystem.gateway.service.grpc;

import io.grpc.Metadata;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import ru.craftysoft.orderingsystem.order.proto.*;
import ru.craftysoft.orderingsystem.util.error.exception.InvocationException;
import ru.craftysoft.orderingsystem.util.grpc.ExceptionHelper;
import ru.craftysoft.orderingsystem.util.grpc.LoggingClientInterceptor;
import ru.craftysoft.orderingsystem.util.grpc.MetadataBuilder;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.util.Optional.ofNullable;
import static ru.craftysoft.orderingsystem.gateway.error.exception.InvocationExceptionCode.ORDER_SERVICE;
import static ru.craftysoft.orderingsystem.gateway.error.operation.ModuleOperationCode.resolve;
import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.newInvocationException;
import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.newRetryableException;
import static ru.craftysoft.orderingsystem.util.grpc.ExceptionHelper.buildKey;
import static ru.craftysoft.orderingsystem.util.grpc.ExceptionHelper.extractErrorResponse;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

@Singleton
@Slf4j
public class OrderServiceClient {

    private final OrderServiceGrpc.OrderServiceStub orderServiceStub;

    private final Metadata.Key<byte[]> getOrdersResponseErrorKey = buildKey(GetOrdersResponse.class);
    private final Metadata.Key<byte[]> addOrderResponseErrorKey = buildKey(AddOrderResponse.class);
    private final Metadata.Key<byte[]> reserveOrderResponseErrorKey = buildKey(ReserveOrderResponse.class);
    private final Function<Throwable, InvocationException> invocationExceptionFactory = throwable -> newInvocationException(throwable, resolve(), ORDER_SERVICE);
    private final Function<Throwable, InvocationException> retryableExceptionFactory = throwable -> newRetryableException(throwable, resolve(), ORDER_SERVICE);

    @Inject
    public OrderServiceClient(OrderServiceGrpc.OrderServiceStub orderServiceStub) {
        this.orderServiceStub = orderServiceStub;
    }

    public CompletableFuture<GetOrdersResponse> getOrders(GetOrdersRequest request) {
        var mdc = MDC.getCopyOfContextMap();
        var metadata = MetadataBuilder.build(mdc);
        var result = new CompletableFuture<GetOrdersResponse>();
        orderServiceStub
                .withInterceptors(new LoggingClientInterceptor(metadata))
                .getOrders(request, new StreamObserver<>() {
                    private GetOrdersResponse response;
                    private final String point = "OrderServiceClient.getOrders";

                    @Override
                    public void onNext(GetOrdersResponse response) {
                        this.response = response;
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        withMdc(mdc, () -> log.error("{}.onError.thrown {}", point, throwable.getMessage()));
                        var baseException = ExceptionHelper.mapException(
                                log, point, mdc, throwable,
                                statusRuntimeException -> extractErrorResponse(statusRuntimeException, getOrdersResponseErrorKey, GetOrdersResponse::parseFrom),
                                invocationExceptionFactory,
                                retryableExceptionFactory,
                                (errorResponse, exception) -> ofNullable(errorResponse)
                                        .filter(GetOrdersResponse::hasError)
                                        .map(GetOrdersResponse::getError)
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

    public CompletableFuture<AddOrderResponse> addOrder(AddOrderRequest request) {
        var mdc = MDC.getCopyOfContextMap();
        var metadata = MetadataBuilder.build(mdc);
        var result = new CompletableFuture<AddOrderResponse>();
        orderServiceStub
                .withInterceptors(new LoggingClientInterceptor(metadata))
                .addOrder(request, new StreamObserver<>() {
                    private AddOrderResponse response;
                    private final String point = "OrderServiceClient.addOrder";

                    @Override
                    public void onNext(AddOrderResponse response) {
                        this.response = response;
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        withMdc(mdc, () -> log.error("{}.onError.thrown {}", point, throwable.getMessage()));
                        var baseException = ExceptionHelper.mapException(
                                log, point, mdc, throwable,
                                statusRuntimeException -> extractErrorResponse(statusRuntimeException, addOrderResponseErrorKey, AddOrderResponse::parseFrom),
                                invocationExceptionFactory,
                                retryableExceptionFactory,
                                (errorResponse, exception) -> ofNullable(errorResponse)
                                        .filter(AddOrderResponse::hasError)
                                        .map(AddOrderResponse::getError)
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

    public CompletableFuture<ReserveOrderResponse> reserveOrder(ReserveOrderRequest request) {
        var mdc = MDC.getCopyOfContextMap();
        var metadata = MetadataBuilder.build(mdc);
        var result = new CompletableFuture<ReserveOrderResponse>();
        orderServiceStub
                .withInterceptors(new LoggingClientInterceptor(metadata))
                .reserveOrder(request, new StreamObserver<>() {
                    private ReserveOrderResponse response;
                    private final String point = "OrderServiceClient.reserveOrder";

                    @Override
                    public void onNext(ReserveOrderResponse response) {
                        this.response = response;
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        withMdc(mdc, () -> log.error("{}.onError.thrown {}", point, throwable.getMessage()));
                        var baseException = ExceptionHelper.mapException(
                                log, point, mdc, throwable,
                                statusRuntimeException -> extractErrorResponse(statusRuntimeException, reserveOrderResponseErrorKey, ReserveOrderResponse::parseFrom),
                                invocationExceptionFactory,
                                retryableExceptionFactory,
                                (errorResponse, exception) -> ofNullable(errorResponse)
                                        .filter(ReserveOrderResponse::hasError)
                                        .map(ReserveOrderResponse::getError)
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
