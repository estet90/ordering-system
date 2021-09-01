package ru.craftysoft.orderingsystem.orderprocessing.logic.messagedriven;

import com.google.type.Money;
import io.grpc.Server;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.craftysoft.orderingsystem.customer.proto.Error;
import ru.craftysoft.orderingsystem.customer.proto.*;
import ru.craftysoft.orderingsystem.orderprocessing.DaggerTestWithoutDbApplicationComponent;
import ru.craftysoft.orderingsystem.orderprocessing.OperationTest;
import ru.craftysoft.orderingsystem.orderprocessing.builder.redis.DecreaseCustomerAmountRequestBuilder;
import ru.craftysoft.orderingsystem.orderprocessing.builder.redis.IncrementExecutorAmountRequestBuilder;
import ru.craftysoft.orderingsystem.orderprocessing.builder.redis.ReserveOrderRequestBuilder;
import ru.craftysoft.orderingsystem.orderprocessing.extension.RedisExtension;
import ru.craftysoft.orderingsystem.orderprocessing.proto.DecreaseCustomerAmountRequest;
import ru.craftysoft.orderingsystem.orderprocessing.proto.RetryData;
import ru.craftysoft.orderingsystem.util.grpc.ExceptionHelper;
import ru.craftysoft.orderingsystem.util.properties.PropertyResolver;
import ru.craftysoft.orderingsystem.util.proto.ProtoUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.craftysoft.orderingsystem.customer.proto.UpdateCustomerBalanceResponseData.Result.BALANCE_HAS_BEEN_CHANGED;
import static ru.craftysoft.orderingsystem.customer.proto.UpdateCustomerBalanceResponseData.Result.BALANCE_HAS_NOT_BEEN_CHANGED;
import static ru.craftysoft.orderingsystem.orderprocessing.error.exception.BusinessExceptionCode.CUSTOMER_BALANCE_HAS_NOT_BEEN_DECREASED;
import static ru.craftysoft.orderingsystem.orderprocessing.error.exception.InvocationExceptionCode.CUSTOMER_SERVICE;
import static ru.craftysoft.orderingsystem.orderprocessing.error.operation.ModuleOperationCode.DECREASE_CUSTOMER_AMOUNT;
import static ru.craftysoft.orderingsystem.orderprocessing.util.StackTraceHelper.listAppender;
import static ru.craftysoft.orderingsystem.orderprocessing.util.StackTraceHelper.thenErrorStacktrace;
import static ru.craftysoft.orderingsystem.util.error.type.ExceptionType.*;

@ExtendWith({
        RedisExtension.class,
})
public class DecreaseCustomerAmountOperationTest extends OperationTest {

    @Inject
    DecreaseCustomerAmountOperation operation;
    @Inject
    DecreaseCustomerAmountRequestBuilder decreaseCustomerAmountRequestBuilder;
    @Inject
    IncrementExecutorAmountRequestBuilder incrementExecutorAmountRequestBuilder;
    @Inject
    ReserveOrderRequestBuilder reserveOrderRequestBuilder;

    private static final String MESSAGE_ID = "messageId";
    private static final Money BALANCE = Money.newBuilder()
            .setUnits(1000)
            .build();
    public static final String ERROR_CODE = "code";
    public static final String ERROR_MESSAGE = "message";

    @Test
    void process() throws Exception {
        var component = DaggerTestWithoutDbApplicationComponent.builder().build();
        component.inject(this);
        redisConsumerGroupInitOperation.process();
        var decreaseCustomerAmountRequest = givenRequest();
        var streamKey = propertyResolver.getStringProperty("redis.stream.decrease-customer-amount.name");
        redisClient.sendMessage(streamKey, MESSAGE_ID, decreaseCustomerAmountRequest, DecreaseCustomerAmountRequest::toByteArray, ProtoUtils::toPrettyString)
                .toCompletableFuture()
                .get();
        var server = customerServiceSuccessServer(propertyResolver);
        serverFuture(server);

        operation.process().toCompletableFuture().get();

        var responseStreamKey = propertyResolver.getStringProperty("redis.stream.increment-executor-amount.name");
        var entries = redisClient.subscribe(responseStreamKey, incrementExecutorAmountRequestBuilder::fromBytes, ProtoUtils::toPrettyString)
                .toCompletableFuture()
                .get();
        assertEquals(1, entries.size());
        var entry = entries.stream().iterator().next();
        assertEquals(MESSAGE_ID, entry.getKey());
        var incrementExecutorAmountRequest = entry.getValue();
        assertEquals(decreaseCustomerAmountRequest.getOrderId(), incrementExecutorAmountRequest.getOrderId());
        assertEquals(decreaseCustomerAmountRequest.getCustomerId(), incrementExecutorAmountRequest.getCustomerId());
        assertEquals(decreaseCustomerAmountRequest.getExecutorId(), incrementExecutorAmountRequest.getExecutorId());
        assertEquals(decreaseCustomerAmountRequest.getAmount(), incrementExecutorAmountRequest.getAmount());
        assertEquals(BALANCE, incrementExecutorAmountRequest.getCustomerBalance());
        server.shutdownNow();
    }

    @Test
    void processBalanceHasNotBeenChanged() throws Exception {
        var component = DaggerTestWithoutDbApplicationComponent.builder().build();
        component.inject(this);
        redisConsumerGroupInitOperation.process();
        var decreaseCustomerAmountRequest = givenRequest();
        var streamKey = propertyResolver.getStringProperty("redis.stream.decrease-customer-amount.name");
        redisClient.sendMessage(streamKey, MESSAGE_ID, decreaseCustomerAmountRequest, DecreaseCustomerAmountRequest::toByteArray, ProtoUtils::toPrettyString)
                .toCompletableFuture()
                .get();
        var server = customerServiceWarningServer(propertyResolver);
        serverFuture(server);
        var listAppender = listAppender(DecreaseCustomerAmountOperation.class);

        operation.process().toCompletableFuture().get();

        var fullErrorCode = fullErrorCode(DECREASE_CUSTOMER_AMOUNT, BUSINESS, CUSTOMER_BALANCE_HAS_NOT_BEEN_DECREASED);
        thenErrorStacktrace(listAppender, fullErrorCode, CUSTOMER_BALANCE_HAS_NOT_BEEN_DECREASED);
        listAppender.stop();
        var responseStreamKey = propertyResolver.getStringProperty("redis.stream.reserve-order.name");
        var entries = redisClient.subscribe(responseStreamKey, reserveOrderRequestBuilder::fromBytes, ProtoUtils::toPrettyString)
                .toCompletableFuture()
                .get();
        assertEquals(1, entries.size());
        var entry = entries.stream().iterator().next();
        assertEquals(MESSAGE_ID, entry.getKey());
        var reserveOrderRequest = entry.getValue();
        assertEquals(decreaseCustomerAmountRequest.getOrderId(), reserveOrderRequest.getOrderId());
        server.shutdownNow();
    }

    @Test
    void processCustomerServiceError() throws Exception {
        var component = DaggerTestWithoutDbApplicationComponent.builder().build();
        component.inject(this);
        redisConsumerGroupInitOperation.process();
        var decreaseCustomerAmountRequest = givenRequest();
        var streamKey = propertyResolver.getStringProperty("redis.stream.decrease-customer-amount.name");
        redisClient.sendMessage(streamKey, MESSAGE_ID, decreaseCustomerAmountRequest, DecreaseCustomerAmountRequest::toByteArray, ProtoUtils::toPrettyString)
                .toCompletableFuture()
                .get();
        var server = customerServiceErrorServer(propertyResolver);
        serverFuture(server);
        var listAppender = listAppender(DecreaseCustomerAmountOperation.class);

        operation.process().toCompletableFuture().get();

        var fullErrorCode = fullErrorCode(DECREASE_CUSTOMER_AMOUNT, INVOCATION, CUSTOMER_SERVICE);
        thenErrorStacktrace(listAppender, fullErrorCode, CUSTOMER_SERVICE);
        listAppender.stop();
        var responseStreamKey = propertyResolver.getStringProperty("redis.stream.reserve-order.name");
        var entries = redisClient.subscribe(responseStreamKey, reserveOrderRequestBuilder::fromBytes, ProtoUtils::toPrettyString)
                .toCompletableFuture()
                .get();
        assertEquals(1, entries.size());
        var entry = entries.stream().iterator().next();
        assertEquals(MESSAGE_ID, entry.getKey());
        var reserveOrderRequest = entry.getValue();
        assertEquals(decreaseCustomerAmountRequest.getOrderId(), reserveOrderRequest.getOrderId());
        server.shutdownNow();
    }

    @Test
    void processCustomerServiceNotAvailable() throws Exception {
        var component = DaggerTestWithoutDbApplicationComponent.builder().build();
        component.inject(this);
        redisConsumerGroupInitOperation.process();
        var decreaseCustomerAmountRequest = givenRequest();
        var streamKey = propertyResolver.getStringProperty("redis.stream.decrease-customer-amount.name");
        redisClient.sendMessage(streamKey, MESSAGE_ID, decreaseCustomerAmountRequest, DecreaseCustomerAmountRequest::toByteArray, ProtoUtils::toPrettyString)
                .toCompletableFuture()
                .get();
        var listAppender = listAppender(DecreaseCustomerAmountOperation.class);

        operation.process().toCompletableFuture().get();

        var fullErrorCode = fullErrorCode(DECREASE_CUSTOMER_AMOUNT, RETRYABLE, CUSTOMER_SERVICE);
        thenErrorStacktrace(listAppender, fullErrorCode, CUSTOMER_SERVICE);
        listAppender.stop();
        var entries = redisClient.subscribe(streamKey, decreaseCustomerAmountRequestBuilder::fromBytes, ProtoUtils::toPrettyString)
                .toCompletableFuture()
                .get();
        assertEquals(1, entries.size());
        var entry = entries.stream().iterator().next();
        assertEquals(MESSAGE_ID, entry.getKey());
        var retryableDecreaseCustomerAmountRequest = entry.getValue();
        assertEquals(decreaseCustomerAmountRequest.getOrderId(), retryableDecreaseCustomerAmountRequest.getOrderId());
        assertEquals(decreaseCustomerAmountRequest.getCustomerId(), retryableDecreaseCustomerAmountRequest.getCustomerId());
        assertEquals(decreaseCustomerAmountRequest.getExecutorId(), retryableDecreaseCustomerAmountRequest.getExecutorId());
        assertEquals(decreaseCustomerAmountRequest.getAmount(), retryableDecreaseCustomerAmountRequest.getAmount());
        assertTrue(retryableDecreaseCustomerAmountRequest.hasRetryData());
        assertEquals(1, retryableDecreaseCustomerAmountRequest.getRetryData().getCounter());
    }

    @Test
    void processRetryExpiry() throws Exception {
        var component = DaggerTestWithoutDbApplicationComponent.builder().build();
        component.inject(this);
        redisConsumerGroupInitOperation.process();
        var decreaseCustomerAmountRequest = DecreaseCustomerAmountRequest.newBuilder(givenRequest())
                .setRetryData(RetryData.newBuilder()
                        .setCounter(propertyResolver.getIntProperty("redis.max-retry-counter"))
                )
                .build();
        var streamKey = propertyResolver.getStringProperty("redis.stream.decrease-customer-amount.name");
        redisClient.sendMessage(streamKey, MESSAGE_ID, decreaseCustomerAmountRequest, DecreaseCustomerAmountRequest::toByteArray, ProtoUtils::toPrettyString)
                .toCompletableFuture()
                .get();
        var listAppender = listAppender(DecreaseCustomerAmountOperation.class);

        operation.process().toCompletableFuture().get();

        var fullErrorCode = fullErrorCode(DECREASE_CUSTOMER_AMOUNT, RETRYABLE, CUSTOMER_SERVICE);
        thenErrorStacktrace(listAppender, fullErrorCode, CUSTOMER_SERVICE);
        listAppender.stop();
        var responseStreamKey = propertyResolver.getStringProperty("redis.stream.reserve-order.name");
        var entries = redisClient.subscribe(responseStreamKey, reserveOrderRequestBuilder::fromBytes, ProtoUtils::toPrettyString)
                .toCompletableFuture()
                .get();
        assertEquals(1, entries.size());
        var entry = entries.stream().iterator().next();
        assertEquals(MESSAGE_ID, entry.getKey());
        var reserveOrderRequest = entry.getValue();
        assertEquals(decreaseCustomerAmountRequest.getOrderId(), reserveOrderRequest.getOrderId());
    }

    private void serverFuture(Server server) {
        CompletableFuture.runAsync(() -> {
            try {
                server.start().awaitTermination();
            } catch (InterruptedException | IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private DecreaseCustomerAmountRequest givenRequest() {
        return DecreaseCustomerAmountRequest.newBuilder()
                .setAmount(Money.newBuilder()
                        .setUnits(10)
                        .setNanos(500_000_000)
                        .build()
                )
                .setCustomerId(1L)
                .setExecutorId(2L)
                .setOrderId(3L)
                .build();
    }

    private Server customerServiceSuccessServer(PropertyResolver propertyResolver) {
        Supplier<UpdateCustomerBalanceResponse> responseBuilder = () -> UpdateCustomerBalanceResponse.newBuilder()
                .setUpdateCustomerBalanceResponseData(UpdateCustomerBalanceResponseData.newBuilder()
                        .setResult(BALANCE_HAS_BEEN_CHANGED)
                        .setBalance(BALANCE)
                )
                .build();
        return customerServiceServer(propertyResolver, responseBuilder);
    }

    private Server customerServiceWarningServer(PropertyResolver propertyResolver) {
        Supplier<UpdateCustomerBalanceResponse> responseBuilder = () -> UpdateCustomerBalanceResponse.newBuilder()
                .setUpdateCustomerBalanceResponseData(UpdateCustomerBalanceResponseData.newBuilder()
                        .setResult(BALANCE_HAS_NOT_BEEN_CHANGED)
                        .setBalance(BALANCE)
                )
                .build();
        return customerServiceServer(propertyResolver, responseBuilder);
    }

    private Server customerServiceErrorServer(PropertyResolver propertyResolver) {
        var controller = new CustomerServiceGrpc.CustomerServiceImplBase() {
            @Override
            public void updateCustomerBalance(UpdateCustomerBalanceRequest request, StreamObserver<UpdateCustomerBalanceResponse> responseObserver) {
                var errorResponse = UpdateCustomerBalanceResponse.newBuilder()
                        .setError(Error.newBuilder()
                                .setCode(ERROR_CODE)
                                .setMessage(ERROR_MESSAGE)
                                .build()
                        )
                        .build();
                responseObserver.onError(ExceptionHelper.messageToException(errorResponse));
            }
        };
        return grpcServer(propertyResolver.getIntProperty("grpc.customer-service.port"), controller);
    }

    private Server customerServiceServer(PropertyResolver propertyResolver, Supplier<UpdateCustomerBalanceResponse> responseBuilder) {
        var controller = new CustomerServiceGrpc.CustomerServiceImplBase() {
            @Override
            public void updateCustomerBalance(UpdateCustomerBalanceRequest request, StreamObserver<UpdateCustomerBalanceResponse> responseObserver) {
                responseObserver.onNext(responseBuilder.get());
                responseObserver.onCompleted();
            }
        };
        return grpcServer(propertyResolver.getIntProperty("grpc.customer-service.port"), controller);
    }
}