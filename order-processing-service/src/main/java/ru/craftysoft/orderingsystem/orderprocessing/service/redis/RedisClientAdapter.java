package ru.craftysoft.orderingsystem.orderprocessing.service.redis;

import com.google.protobuf.AbstractMessageLite;
import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.customer.proto.UpdateCustomerBalanceResponse;
import ru.craftysoft.orderingsystem.orderprocessing.builder.redis.*;
import ru.craftysoft.orderingsystem.orderprocessing.dto.Order;
import ru.craftysoft.orderingsystem.orderprocessing.error.exception.RetryExpiryException;
import ru.craftysoft.orderingsystem.orderprocessing.proto.*;
import ru.craftysoft.orderingsystem.util.properties.PropertyResolver;
import ru.craftysoft.orderingsystem.util.proto.ProtoUtils;
import ru.craftysoft.orderingsystem.util.uuid.UuidUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;

@Singleton
@Slf4j
public class RedisClientAdapter {

    private final RedisClient client;
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
                              DecreaseCustomerAmountRequestBuilder decreaseCustomerAmountRequestBuilder,
                              DecreaseExecutorAmountRequestBuilder decreaseExecutorAmountRequestBuilder,
                              IncrementExecutorAmountRequestBuilder incrementExecutorAmountRequestBuilder,
                              IncrementCustomerAmountRequestBuilder incrementCustomerAmountRequestBuilder,
                              ReserveOrderRequestBuilder reserveOrderRequestBuilder,
                              CompleteOrderRequestBuilder completeOrderRequestBuilder,
                              PropertyResolver propertyResolver) {
        this.client = client;
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

    public CompletionStage<String> sendMessagesToDecreaseCustomerAmountStream(Order order) {
        var request = decreaseCustomerAmountRequestBuilder.build(order);
        return client.sendMessage(
                decreaseCustomerAmountStream, UuidUtils.generateDefaultUuid(), request,
                AbstractMessageLite::toByteArray, ProtoUtils::toPrettyString
        );
    }

    public CompletionStage<String> sendMessageToIncrementExecutorAmountStream(UpdateCustomerBalanceResponse updateCustomerBalanceResponse,
                                                                              Map.Entry<String, DecreaseCustomerAmountRequest> entry) {
        var request = incrementExecutorAmountRequestBuilder.build(updateCustomerBalanceResponse, entry.getValue());
        return client.sendMessage(
                incrementExecutorAmountStream, entry.getKey(), request,
                AbstractMessageLite::toByteArray, ProtoUtils::toPrettyString
        );
    }

    public CompletionStage<String> sendMessageToReserveOrderStream(Map.Entry<String, DecreaseCustomerAmountRequest> entry) {
        var request = reserveOrderRequestBuilder.build(entry.getValue());
        return client.sendMessage(
                reserveOrderStream, entry.getKey(), request,
                AbstractMessageLite::toByteArray, ProtoUtils::toPrettyString
        );
    }

    public CompletionStage<String> sendMessageToIncrementCustomerAmountStream(Map.Entry<String, IncrementExecutorAmountRequest> entry) {
        var request = incrementCustomerAmountRequestBuilder.build(entry.getValue());
        return client.sendMessage(
                incrementCustomerAmountStream, entry.getKey(), request,
                AbstractMessageLite::toByteArray, ProtoUtils::toPrettyString
        );
    }

    public CompletionStage<String> sendMessageToIncrementCustomerAmountStreamInRollback(Map.Entry<String, DecreaseExecutorAmountRequest> entry) {
        var request = incrementCustomerAmountRequestBuilder.build(entry.getValue());
        return client.sendMessage(
                incrementCustomerAmountStream, entry.getKey(), request,
                AbstractMessageLite::toByteArray, ProtoUtils::toPrettyString
        );
    }

    public CompletionStage<String> sendMessageToDecreaseExecutorAmountStream(Map.Entry<String, CompleteOrderRequest> entry) {
        var request = decreaseExecutorAmountRequestBuilder.build(entry.getValue());
        return client.sendMessage(
                decreaseExecutorAmountStream, entry.getKey(), request,
                AbstractMessageLite::toByteArray, ProtoUtils::toPrettyString
        );
    }

    public CompletionStage<String> sendMessageToCompleteOrderStream(Map.Entry<String, IncrementExecutorAmountRequest> entry) {
        var request = completeOrderRequestBuilder.build(entry.getValue());
        return client.sendMessage(
                completeOrderStream, entry.getKey(), request,
                AbstractMessageLite::toByteArray, ProtoUtils::toPrettyString
        );
    }

    public CompletionStage<String> sendMessageToReserveOrderStreamInRollback(Map.Entry<String, IncrementCustomerAmountRequest> entry) {
        var request = reserveOrderRequestBuilder.build(entry.getValue());
        return client.sendMessage(
                reserveOrderStream, entry.getKey(), request,
                AbstractMessageLite::toByteArray, ProtoUtils::toPrettyString
        );
    }

    public CompletionStage<List<Map.Entry<String, DecreaseCustomerAmountRequest>>> listenDecreaseCustomerAmountRequestMessages() {
        return client.subscribe(decreaseCustomerAmountStream, decreaseCustomerAmountRequestBuilder::fromBytes, ProtoUtils::toPrettyString);
    }

    public CompletionStage<List<Map.Entry<String, IncrementExecutorAmountRequest>>> listenIncrementExecutorAmountRequestMessages() {
        return client.subscribe(incrementExecutorAmountStream, incrementExecutorAmountRequestBuilder::fromBytes, ProtoUtils::toPrettyString);
    }

    public CompletionStage<List<Map.Entry<String, ReserveOrderRequest>>> listenReserveOrderRequestMessages() {
        return client.subscribe(reserveOrderStream, reserveOrderRequestBuilder::fromBytes, ProtoUtils::toPrettyString);
    }

    public CompletionStage<List<Map.Entry<String, CompleteOrderRequest>>> listenCompleteOrderRequestMessages() {
        return client.subscribe(completeOrderStream, completeOrderRequestBuilder::fromBytes, ProtoUtils::toPrettyString);
    }

    public CompletionStage<List<Map.Entry<String, DecreaseExecutorAmountRequest>>> listenDecreaseExecutorAmountRequestMessages() {
        return client.subscribe(decreaseExecutorAmountStream, decreaseExecutorAmountRequestBuilder::fromBytes, ProtoUtils::toPrettyString);
    }

    public CompletionStage<List<Map.Entry<String, IncrementCustomerAmountRequest>>> listenIncrementCustomerAmountRequestMessages() {
        return client.subscribe(incrementCustomerAmountStream, incrementCustomerAmountRequestBuilder::fromBytes, ProtoUtils::toPrettyString);
    }

    public CompletionStage<String> retryDecreaseCustomerAmountRequestMessage(Map.Entry<String, DecreaseCustomerAmountRequest> entry,
                                                                             Throwable throwable) {
        var request = entry.getValue();
        int counter = resolveCounter(request, DecreaseCustomerAmountRequest::hasRetryData, rq -> rq.getRetryData().getCounter());
        if (counter < maxRetryCounter) {
            var newRequest = decreaseCustomerAmountRequestBuilder.build(request, counter);
            return client.sendMessage(
                    decreaseCustomerAmountStream, entry.getKey(), newRequest,
                    AbstractMessageLite::toByteArray, ProtoUtils::toPrettyString
            );
        } else {
            return CompletableFuture.failedFuture(new RetryExpiryException(throwable));
        }
    }

    public CompletionStage<String> retryIncrementExecutorAmountRequestMessage(Map.Entry<String, IncrementExecutorAmountRequest> entry,
                                                                              Throwable throwable) {
        var request = entry.getValue();
        int counter = resolveCounter(request, IncrementExecutorAmountRequest::hasRetryData, rq -> rq.getRetryData().getCounter());
        if (counter < maxRetryCounter) {
            var newRequest = incrementExecutorAmountRequestBuilder.build(request, counter);
            return client.sendMessage(
                    decreaseExecutorAmountStream, entry.getKey(), newRequest,
                    AbstractMessageLite::toByteArray, ProtoUtils::toPrettyString
            );
        } else {
            return CompletableFuture.failedFuture(new RetryExpiryException(throwable));
        }
    }

    public CompletionStage<String> retryReserveOrderRequestMessage(Map.Entry<String, ReserveOrderRequest> entry,
                                                                   Throwable throwable) {
        var request = entry.getValue();
        int counter = resolveCounter(request, ReserveOrderRequest::hasRetryData, rq -> rq.getRetryData().getCounter());
        if (counter < maxRetryCounter) {
            var newRequest = reserveOrderRequestBuilder.build(request, counter);
            return client.sendMessage(
                    reserveOrderStream, entry.getKey(), newRequest,
                    AbstractMessageLite::toByteArray, ProtoUtils::toPrettyString
            );
        } else {
            return CompletableFuture.failedFuture(new RetryExpiryException(throwable));
        }
    }

    public CompletionStage<String> retryCompleteOrderRequestMessage(Map.Entry<String, CompleteOrderRequest> entry,
                                                                    Throwable throwable) {
        var request = entry.getValue();
        int counter = resolveCounter(request, CompleteOrderRequest::hasRetryData, rq -> rq.getRetryData().getCounter());
        if (counter < maxRetryCounter) {
            var newRequest = completeOrderRequestBuilder.build(request, counter);
            return client.sendMessage(
                    completeOrderStream, entry.getKey(), newRequest,
                    AbstractMessageLite::toByteArray, ProtoUtils::toPrettyString
            );
        } else {
            return CompletableFuture.failedFuture(new RetryExpiryException(throwable));
        }
    }

    public CompletionStage<String> retryDecreaseExecutorAmountRequestMessage(Map.Entry<String, DecreaseExecutorAmountRequest> entry,
                                                                             Throwable throwable) {
        var request = entry.getValue();
        int counter = resolveCounter(request, DecreaseExecutorAmountRequest::hasRetryData, rq -> rq.getRetryData().getCounter());
        if (counter < maxRetryCounter) {
            var newRequest = decreaseExecutorAmountRequestBuilder.build(request, counter);
            return client.sendMessage(
                    decreaseExecutorAmountStream, entry.getKey(), newRequest,
                    AbstractMessageLite::toByteArray, ProtoUtils::toPrettyString
            );
        } else {
            return CompletableFuture.failedFuture(new RetryExpiryException(throwable));
        }
    }

    public CompletionStage<String> retryIncrementCustomerAmountRequestMessage(Map.Entry<String, IncrementCustomerAmountRequest> entry,
                                                                              Throwable throwable) {
        var request = entry.getValue();
        int counter = resolveCounter(request, IncrementCustomerAmountRequest::hasRetryData, rq -> rq.getRetryData().getCounter());
        if (counter < maxRetryCounter) {
            var newRequest = incrementCustomerAmountRequestBuilder.build(request, counter);
            return client.sendMessage(
                    incrementCustomerAmountStream, entry.getKey(), newRequest,
                    AbstractMessageLite::toByteArray, ProtoUtils::toPrettyString
            );
        } else {
            return CompletableFuture.failedFuture(new RetryExpiryException(throwable));
        }
    }

    private <T> int resolveCounter(T request, Predicate<T> hasRetryData, Function<T, Integer> counterExtractor) {
        if (!hasRetryData.test(request)) {
            return 1;
        }
        var counter = counterExtractor.apply(request);
        counter++;
        return counter;
    }
}
