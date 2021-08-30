package ru.craftysoft.orderingsystem.gateway.service.grpc;

import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import ru.craftysoft.orderingsystem.customer.proto.CustomerServiceGrpc;
import ru.craftysoft.orderingsystem.customer.proto.GetCustomerRequest;
import ru.craftysoft.orderingsystem.customer.proto.GetCustomerResponse;
import ru.craftysoft.orderingsystem.util.error.exception.BaseException;
import ru.craftysoft.orderingsystem.util.error.exception.InvocationException;
import ru.craftysoft.orderingsystem.util.grpc.LoggingClientInterceptor;
import ru.craftysoft.orderingsystem.util.grpc.MetadataBuilder;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.util.Optional.ofNullable;
import static ru.craftysoft.orderingsystem.gateway.error.exception.InvocationExceptionCode.CUSTOMER_SERVICE;
import static ru.craftysoft.orderingsystem.gateway.error.operation.ModuleOperationCode.resolve;
import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.newInvocationException;
import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.newRetryableException;
import static ru.craftysoft.orderingsystem.util.grpc.ExceptionHelper.*;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

@Singleton
@Slf4j
public class CustomerServiceClient {

    private final CustomerServiceGrpc.CustomerServiceStub customerServiceStub;
    private final Metadata.Key<byte[]> getCustomerResponseErrorKey = buildKey(GetCustomerResponse.class);
    private final Function<StatusRuntimeException, GetCustomerResponse> errorResponseBuilder = statusRuntimeException -> extractErrorResponse(statusRuntimeException, getCustomerResponseErrorKey, GetCustomerResponse::parseFrom);
    private final Function<Throwable, InvocationException> invocationExceptionFactory = throwable -> newInvocationException(throwable, resolve(), CUSTOMER_SERVICE);
    private final Function<Throwable, InvocationException> retryableExceptionFactory = throwable -> newRetryableException(throwable, resolve(), CUSTOMER_SERVICE);
    private final BiConsumer<GetCustomerResponse, BaseException> baseExceptionFiller = (errorResponse, baseException) -> ofNullable(errorResponse)
            .filter(GetCustomerResponse::hasError)
            .map(GetCustomerResponse::getError)
            .ifPresent(error -> baseException
                    .setOriginalCode(error.getCode())
                    .setOriginalMessage(error.getMessage()));

    @Inject
    public CustomerServiceClient(CustomerServiceGrpc.CustomerServiceStub customerServiceStub) {
        this.customerServiceStub = customerServiceStub;
    }

    public CompletableFuture<GetCustomerResponse> getCustomer(GetCustomerRequest request) {
        var mdc = MDC.getCopyOfContextMap();
        var metadata = MetadataBuilder.build(mdc);
        var result = new CompletableFuture<GetCustomerResponse>();
        customerServiceStub
                .withInterceptors(new LoggingClientInterceptor(metadata))
                .getCustomer(request, new StreamObserver<>() {
                    private GetCustomerResponse response;
                    private final String point = "CustomerServiceClient.getCustomer";

                    @Override
                    public void onNext(GetCustomerResponse response) {
                        this.response = response;
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        withMdc(mdc, () -> log.error("{}.onError.thrown {}", point, throwable.getMessage()));
                        var baseException = mapException(
                                log, point, mdc, throwable,
                                errorResponseBuilder, invocationExceptionFactory, retryableExceptionFactory, baseExceptionFiller
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
