package ru.craftysoft.orderingsystem.orderprocessing.service.redis;

import com.google.protobuf.AbstractMessageLite;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.util.JsonFormat;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.support.BoundedAsyncPool;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.customer.proto.UpdateCustomerBalanceResponse;
import ru.craftysoft.orderingsystem.orderprocessing.builder.redis.*;
import ru.craftysoft.orderingsystem.orderprocessing.dto.Order;
import ru.craftysoft.orderingsystem.orderprocessing.error.exception.RetryExpiryException;
import ru.craftysoft.orderingsystem.orderprocessing.proto.*;
import ru.craftysoft.orderingsystem.util.properties.PropertyResolver;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;

@Singleton
@Slf4j
public class RedisClientAdapter {

    private final RedisClient client;
    private final BoundedAsyncPool<StatefulRedisConnection<String, byte[]>> redisPool;
    private final DecreaseCustomerAmountRequestBuilder decreaseCustomerAmountRequestBuilder;
    private final DecreaseExecutorAmountRequestBuilder decreaseExecutorAmountRequestBuilder;
    private final IncrementExecutorAmountRequestBuilder incrementExecutorAmountRequestBuilder;
    private final IncrementCustomerAmountRequestBuilder incrementCustomerAmountRequestBuilder;
    private final ReserveOrderRequestBuilder reserveOrderRequestBuilder;
    private final CompleteOrderRequestBuilder completeOrderRequestBuilder;
    private final String decreaseCustomerAmountStream;
    private final String incrementCustomerAmountStream;
    private final String decreaseExecutorAmountStream;
    private final String incrementExecutorAmountStream;
    private final String reserveOrderStream;
    private final String completeOrderStream;
    private final int maxRetryCounter;

    @Inject
    public RedisClientAdapter(RedisClient client,
                              BoundedAsyncPool<StatefulRedisConnection<String, byte[]>> redisPool,
                              DecreaseCustomerAmountRequestBuilder decreaseCustomerAmountRequestBuilder,
                              DecreaseExecutorAmountRequestBuilder decreaseExecutorAmountRequestBuilder,
                              IncrementExecutorAmountRequestBuilder incrementExecutorAmountRequestBuilder,
                              IncrementCustomerAmountRequestBuilder incrementCustomerAmountRequestBuilder,
                              ReserveOrderRequestBuilder reserveOrderRequestBuilder,
                              CompleteOrderRequestBuilder completeOrderRequestBuilder,
                              PropertyResolver propertyResolver) {
        this.client = client;
        this.redisPool = redisPool;
        this.decreaseCustomerAmountRequestBuilder = decreaseCustomerAmountRequestBuilder;
        this.decreaseExecutorAmountRequestBuilder = decreaseExecutorAmountRequestBuilder;
        this.incrementExecutorAmountRequestBuilder = incrementExecutorAmountRequestBuilder;
        this.incrementCustomerAmountRequestBuilder = incrementCustomerAmountRequestBuilder;
        this.reserveOrderRequestBuilder = reserveOrderRequestBuilder;
        this.completeOrderRequestBuilder = completeOrderRequestBuilder;
        this.decreaseCustomerAmountStream = propertyResolver.getStringProperty("redis.stream.decrease-customer-amount.name");
        this.incrementCustomerAmountStream = propertyResolver.getStringProperty("redis.stream.increment-customer-amount.name");
        this.decreaseExecutorAmountStream = propertyResolver.getStringProperty("redis.stream.decrease-executor-amount.name");
        this.incrementExecutorAmountStream = propertyResolver.getStringProperty("redis.stream.increment-executor-amount.name");
        this.reserveOrderStream = propertyResolver.getStringProperty("redis.stream.reserve-order.name");
        this.completeOrderStream = propertyResolver.getStringProperty("redis.stream.complete-order.name");
        this.maxRetryCounter = propertyResolver.getIntProperty("redis.max-retry-counter");
    }

    public CompletableFuture<Void> sendMessagesToDecreaseCustomerAmountStream(Order order) {
        var request = decreaseCustomerAmountRequestBuilder.build(order);
        return redisPool.acquire()
                .thenAccept(connection -> client.sendMessage(
                        connection, decreaseCustomerAmountStream, request,
                        AbstractMessageLite::toByteArray, this::toPrettyString
                ));
    }

    public CompletableFuture<Void> sendMessageToIncrementExecutorAmountStream(UpdateCustomerBalanceResponse updateCustomerBalanceResponse,
                                                                              DecreaseCustomerAmountRequest decreaseCustomerAmountRequest) {
        var request = incrementExecutorAmountRequestBuilder.build(updateCustomerBalanceResponse, decreaseCustomerAmountRequest);
        return redisPool.acquire()
                .thenAccept(connection -> client.sendMessage(
                        connection, incrementExecutorAmountStream, request,
                        AbstractMessageLite::toByteArray, this::toPrettyString
                ));
    }

    public CompletableFuture<Void> sendMessageToReserveOrderStream(DecreaseCustomerAmountRequest decreaseCustomerAmountRequest) {
        var request = reserveOrderRequestBuilder.build(decreaseCustomerAmountRequest);
        return redisPool.acquire()
                .thenAccept(connection -> client.sendMessage(
                        connection, reserveOrderStream, request,
                        AbstractMessageLite::toByteArray, this::toPrettyString
                ));
    }

    public CompletableFuture<Void> sendMessageToIncrementCustomerAmountStream(IncrementExecutorAmountRequest incrementExecutorAmountRequest) {
        var request = incrementCustomerAmountRequestBuilder.build(incrementExecutorAmountRequest);
        return redisPool.acquire()
                .thenAccept(connection -> client.sendMessage(
                        connection, incrementCustomerAmountStream, request,
                        AbstractMessageLite::toByteArray, this::toPrettyString
                ));
    }

    public CompletableFuture<Void> sendMessageToIncrementCustomerAmountStream(DecreaseExecutorAmountRequest incrementExecutorAmountRequest) {
        var request = incrementCustomerAmountRequestBuilder.build(incrementExecutorAmountRequest);
        return redisPool.acquire()
                .thenAccept(connection -> client.sendMessage(
                        connection, incrementCustomerAmountStream, request,
                        AbstractMessageLite::toByteArray, this::toPrettyString
                ));
    }

    public CompletableFuture<Void> sendMessageToDecreaseExecutorAmountStream(CompleteOrderRequest completeOrderRequest) {
        var request = decreaseExecutorAmountRequestBuilder.build(completeOrderRequest);
        return redisPool.acquire()
                .thenAccept(connection -> client.sendMessage(
                        connection, decreaseExecutorAmountStream, request,
                        AbstractMessageLite::toByteArray, this::toPrettyString
                ));
    }

    public CompletableFuture<Void> sendMessageToCompleteOrderStream(IncrementExecutorAmountRequest incrementExecutorAmountRequest) {
        var request = completeOrderRequestBuilder.build(incrementExecutorAmountRequest);
        return redisPool.acquire()
                .thenAccept(connection -> client.sendMessage(
                        connection, completeOrderStream, request,
                        AbstractMessageLite::toByteArray, this::toPrettyString
                ));
    }

    public CompletionStage<Void> sendMessageToReserveOrderStream(IncrementCustomerAmountRequest incrementCustomerAmountRequest) {
        var request = reserveOrderRequestBuilder.build(incrementCustomerAmountRequest);
        return redisPool.acquire()
                .thenAccept(connection -> client.sendMessage(
                        connection, reserveOrderStream, request,
                        AbstractMessageLite::toByteArray, this::toPrettyString
                ));
    }

    public CompletableFuture<List<DecreaseCustomerAmountRequest>> listenDecreaseCustomerAmountRequestMessages() {
        return redisPool.acquire()
                .thenCompose(connection -> client.subscribe(
                                        connection, decreaseCustomerAmountStream,
                                        decreaseCustomerAmountRequestBuilder::fromBytes, this::toPrettyString
                                )
                                .whenComplete((requests, throwable) -> redisPool.release(connection))
                );
    }

    public CompletableFuture<List<IncrementExecutorAmountRequest>> listenIncrementExecutorAmountRequestMessages() {
        return redisPool.acquire()
                .thenCompose(connection -> client.subscribe(
                                        connection, incrementExecutorAmountStream,
                                        incrementExecutorAmountRequestBuilder::fromBytes, this::toPrettyString
                                )
                                .whenComplete((requests, throwable) -> redisPool.release(connection))
                );
    }

    public CompletableFuture<List<ReserveOrderRequest>> listenReserveOrderRequestMessages() {
        return redisPool.acquire()
                .thenCompose(connection -> client.subscribe(
                                        connection, reserveOrderStream,
                                        reserveOrderRequestBuilder::fromBytes, this::toPrettyString
                                )
                                .whenComplete((requests, throwable) -> redisPool.release(connection))
                );
    }

    public CompletableFuture<List<CompleteOrderRequest>> listenCompleteOrderRequestMessages() {
        return redisPool.acquire()
                .thenCompose(connection -> client.subscribe(
                                        connection, completeOrderStream,
                                        completeOrderRequestBuilder::fromBytes, this::toPrettyString
                                )
                                .whenComplete((requests, throwable) -> redisPool.release(connection))
                );
    }

    public CompletableFuture<List<DecreaseExecutorAmountRequest>> listenDecreaseExecutorAmountRequestMessages() {
        return redisPool.acquire()
                .thenCompose(connection -> client.subscribe(
                                        connection, decreaseExecutorAmountStream,
                                        decreaseExecutorAmountRequestBuilder::fromBytes, this::toPrettyString
                                )
                                .whenComplete((requests, throwable) -> redisPool.release(connection))
                );
    }

    public CompletableFuture<List<IncrementCustomerAmountRequest>> listenIncrementCustomerAmountRequestMessages() {
        return redisPool.acquire()
                .thenCompose(connection -> client.subscribe(
                                        connection, incrementCustomerAmountStream,
                                        incrementCustomerAmountRequestBuilder::fromBytes, this::toPrettyString
                                )
                                .whenComplete((requests, throwable) -> redisPool.release(connection))
                );
    }

    public CompletableFuture<Void> retryDecreaseCustomerAmountRequestMessage(DecreaseCustomerAmountRequest request,
                                                                             Throwable throwable) {
        return redisPool.acquire()
                .thenAccept(connection -> {
                    int counter = resolveCounter(request, DecreaseCustomerAmountRequest::hasRetryData, rq -> rq.getRetryData().getCounter());
                    if (counter < maxRetryCounter) {
                        var newRequest = decreaseCustomerAmountRequestBuilder.build(request, counter);
                        client.sendMessage(
                                connection, decreaseCustomerAmountStream, newRequest,
                                AbstractMessageLite::toByteArray, this::toPrettyString
                        );
                    } else {
                        throw new RetryExpiryException(throwable);
                    }
                });
    }

    public CompletableFuture<Void> retryIncrementExecutorAmountRequestMessage(IncrementExecutorAmountRequest request,
                                                                              Throwable throwable) {
        return redisPool.acquire()
                .thenAccept(connection -> {
                    int counter = resolveCounter(request, IncrementExecutorAmountRequest::hasRetryData, rq -> rq.getRetryData().getCounter());
                    if (counter < maxRetryCounter) {
                        var newRequest = incrementExecutorAmountRequestBuilder.build(request, counter);
                        client.sendMessage(
                                connection, decreaseExecutorAmountStream, newRequest,
                                AbstractMessageLite::toByteArray, this::toPrettyString
                        );
                    } else {
                        throw new RetryExpiryException(throwable);
                    }
                });
    }

    public CompletableFuture<Void> retryReserveOrderRequestMessage(ReserveOrderRequest request,
                                                                   Throwable throwable) {
        return redisPool.acquire()
                .thenAccept(connection -> {
                    int counter = resolveCounter(request, ReserveOrderRequest::hasRetryData, rq -> rq.getRetryData().getCounter());
                    if (counter < maxRetryCounter) {
                        var newRequest = reserveOrderRequestBuilder.build(request, counter);
                        client.sendMessage(
                                connection, reserveOrderStream, newRequest,
                                AbstractMessageLite::toByteArray, this::toPrettyString
                        );
                    } else {
                        throw new RetryExpiryException(throwable);
                    }
                });
    }

    public CompletionStage<Void> retryCompleteOrderRequestMessage(CompleteOrderRequest request,
                                                                  Throwable throwable) {
        return redisPool.acquire()
                .thenAccept(connection -> {
                    int counter = resolveCounter(request, CompleteOrderRequest::hasRetryData, rq -> rq.getRetryData().getCounter());
                    if (counter < maxRetryCounter) {
                        var newRequest = completeOrderRequestBuilder.build(request, counter);
                        client.sendMessage(
                                connection, completeOrderStream, newRequest,
                                AbstractMessageLite::toByteArray, this::toPrettyString
                        );
                    } else {
                        throw new RetryExpiryException(throwable);
                    }
                });
    }

    public CompletionStage<Void> retryDecreaseExecutorAmountRequestMessage(DecreaseExecutorAmountRequest request, Throwable throwable) {
        return redisPool.acquire()
                .thenAccept(connection -> {
                    int counter = resolveCounter(request, DecreaseExecutorAmountRequest::hasRetryData, rq -> rq.getRetryData().getCounter());
                    if (counter < maxRetryCounter) {
                        var newRequest = decreaseExecutorAmountRequestBuilder.build(request, counter);
                        client.sendMessage(
                                connection, decreaseExecutorAmountStream, newRequest,
                                AbstractMessageLite::toByteArray, this::toPrettyString
                        );
                    } else {
                        throw new RetryExpiryException(throwable);
                    }
                });
    }

    public CompletionStage<Void> retryIncrementCustomerAmountRequestMessage(IncrementCustomerAmountRequest request, Throwable throwable) {
        return redisPool.acquire()
                .thenAccept(connection -> {
                    int counter = resolveCounter(request, IncrementCustomerAmountRequest::hasRetryData, rq -> rq.getRetryData().getCounter());
                    if (counter < maxRetryCounter) {
                        var newRequest = incrementCustomerAmountRequestBuilder.build(request, counter);
                        client.sendMessage(
                                connection, incrementCustomerAmountStream, newRequest,
                                AbstractMessageLite::toByteArray, this::toPrettyString
                        );
                    } else {
                        throw new RetryExpiryException(throwable);
                    }
                });
    }

    private <T> int resolveCounter(T request, Predicate<T> hasRetryData, Function<T, Integer> counterExtractor) {
        if (!hasRetryData.test(request)) {
            return 1;
        }
        var counter = counterExtractor.apply(request);
        counter++;
        return counter;
    }

    @SneakyThrows
    private <T extends GeneratedMessageV3> String toPrettyString(T entity) {
        return JsonFormat.printer().print(entity);
    }
}
