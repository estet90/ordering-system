package ru.craftysoft.orderingsystem.orderprocessing.service.redis;

import com.google.protobuf.AbstractMessageLite;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.support.BoundedAsyncPool;
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

import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

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
                .thenAccept(withMdc(connection -> {
                    client.sendMessage(
                                    connection, decreaseCustomerAmountStream, UuidUtils.generateDefaultUuid(), request,
                                    AbstractMessageLite::toByteArray, ProtoUtils::toPrettyString
                            )
                            .whenComplete(withMdc((requests, throwable) -> {
                                redisPool.release(connection);
                            }));
                }));
    }

    public CompletableFuture<Void> sendMessageToIncrementExecutorAmountStream(UpdateCustomerBalanceResponse updateCustomerBalanceResponse,
                                                                              Map.Entry<String, DecreaseCustomerAmountRequest> entry) {
        var request = incrementExecutorAmountRequestBuilder.build(updateCustomerBalanceResponse, entry.getValue());
        return redisPool.acquire()
                .thenAccept(withMdc(connection -> {
                    client.sendMessage(
                                    connection, incrementExecutorAmountStream, entry.getKey(), request,
                                    AbstractMessageLite::toByteArray, ProtoUtils::toPrettyString
                            )
                            .whenComplete(withMdc((requests, throwable) -> {
                                redisPool.release(connection);
                            }));
                }));
    }

    public CompletableFuture<Void> sendMessageToReserveOrderStream(Map.Entry<String, DecreaseCustomerAmountRequest> entry) {
        var request = reserveOrderRequestBuilder.build(entry.getValue());
        return redisPool.acquire()
                .thenAccept(withMdc(connection -> {
                    client.sendMessage(
                                    connection, reserveOrderStream, entry.getKey(), request,
                                    AbstractMessageLite::toByteArray, ProtoUtils::toPrettyString
                            )
                            .whenComplete(withMdc((requests, throwable) -> {
                                redisPool.release(connection);
                            }));
                }));
    }

    public CompletableFuture<Void> sendMessageToIncrementCustomerAmountStream(Map.Entry<String, IncrementExecutorAmountRequest> entry) {
        var request = incrementCustomerAmountRequestBuilder.build(entry.getValue());
        return redisPool.acquire()
                .thenAccept(withMdc(connection -> {
                    client.sendMessage(
                                    connection, incrementCustomerAmountStream, entry.getKey(), request,
                                    AbstractMessageLite::toByteArray, ProtoUtils::toPrettyString
                            )
                            .whenComplete(withMdc((requests, throwable) -> {
                                redisPool.release(connection);
                            }));
                }));
    }

    public CompletableFuture<Void> sendMessageToIncrementCustomerAmountStreamInRollback(Map.Entry<String, DecreaseExecutorAmountRequest> entry) {
        var request = incrementCustomerAmountRequestBuilder.build(entry.getValue());
        return redisPool.acquire()
                .thenAccept(withMdc(connection -> {
                    client.sendMessage(
                                    connection, incrementCustomerAmountStream, entry.getKey(), request,
                                    AbstractMessageLite::toByteArray, ProtoUtils::toPrettyString
                            )
                            .whenComplete(withMdc((requests, throwable) -> {
                                redisPool.release(connection);
                            }));
                }));
    }

    public CompletableFuture<Void> sendMessageToDecreaseExecutorAmountStream(Map.Entry<String, CompleteOrderRequest> entry) {
        var request = decreaseExecutorAmountRequestBuilder.build(entry.getValue());
        return redisPool.acquire()
                .thenAccept(withMdc(connection -> {
                    client.sendMessage(
                                    connection, decreaseExecutorAmountStream, entry.getKey(), request,
                                    AbstractMessageLite::toByteArray, ProtoUtils::toPrettyString
                            )
                            .whenComplete(withMdc((requests, throwable) -> {
                                redisPool.release(connection);
                            }));
                }));
    }

    public CompletableFuture<Void> sendMessageToCompleteOrderStream(Map.Entry<String, IncrementExecutorAmountRequest> entry) {
        var request = completeOrderRequestBuilder.build(entry.getValue());
        return redisPool.acquire()
                .thenAccept(withMdc(connection -> {
                    client.sendMessage(
                                    connection, completeOrderStream, entry.getKey(), request,
                                    AbstractMessageLite::toByteArray, ProtoUtils::toPrettyString
                            )
                            .whenComplete(withMdc((requests, throwable) -> {
                                redisPool.release(connection);
                            }));
                }));
    }

    public CompletionStage<Void> sendMessageToReserveOrderStreamInRollback(Map.Entry<String, IncrementCustomerAmountRequest> entry) {
        var request = reserveOrderRequestBuilder.build(entry.getValue());
        return redisPool.acquire()
                .thenAccept(withMdc(connection -> {
                    client.sendMessage(
                                    connection, reserveOrderStream, entry.getKey(), request,
                                    AbstractMessageLite::toByteArray, ProtoUtils::toPrettyString
                            )
                            .whenComplete(withMdc((requests, throwable) -> {
                                redisPool.release(connection);
                            }));
                }));
    }

    public CompletableFuture<List<Map.Entry<String, DecreaseCustomerAmountRequest>>> listenDecreaseCustomerAmountRequestMessages() {
        return redisPool.acquire()
                .thenCompose(withMdc(connection -> {
                            return client.subscribe(
                                            connection, decreaseCustomerAmountStream,
                                            decreaseCustomerAmountRequestBuilder::fromBytes, ProtoUtils::toPrettyString
                                    )
                                    .whenComplete(withMdc((requests, throwable) -> {
                                        redisPool.release(connection);
                                    }));
                        }
                ));
    }

    public CompletableFuture<List<Map.Entry<String, IncrementExecutorAmountRequest>>> listenIncrementExecutorAmountRequestMessages() {
        return redisPool.acquire()
                .thenCompose(withMdc(connection -> {
                            return client.subscribe(
                                            connection, incrementExecutorAmountStream,
                                            incrementExecutorAmountRequestBuilder::fromBytes, ProtoUtils::toPrettyString
                                    )
                                    .whenComplete(withMdc((requests, throwable) -> {
                                        redisPool.release(connection);
                                    }));
                        }
                ));
    }

    public CompletableFuture<List<Map.Entry<String, ReserveOrderRequest>>> listenReserveOrderRequestMessages() {
        return redisPool.acquire()
                .thenCompose(withMdc(connection -> {
                            return client.subscribe(
                                            connection, reserveOrderStream,
                                            reserveOrderRequestBuilder::fromBytes, ProtoUtils::toPrettyString
                                    )
                                    .whenComplete(withMdc((requests, throwable) -> {
                                        redisPool.release(connection);
                                    }));
                        }
                ));
    }

    public CompletableFuture<List<Map.Entry<String, CompleteOrderRequest>>> listenCompleteOrderRequestMessages() {
        return redisPool.acquire()
                .thenCompose(withMdc(connection -> {
                            return client.subscribe(
                                            connection, completeOrderStream,
                                            completeOrderRequestBuilder::fromBytes, ProtoUtils::toPrettyString
                                    )
                                    .whenComplete(withMdc((requests, throwable) -> {
                                        redisPool.release(connection);
                                    }));
                        }
                ));
    }

    public CompletableFuture<List<Map.Entry<String, DecreaseExecutorAmountRequest>>> listenDecreaseExecutorAmountRequestMessages() {
        return redisPool.acquire()
                .thenCompose(withMdc(connection -> {
                            return client.subscribe(
                                            connection, decreaseExecutorAmountStream,
                                            decreaseExecutorAmountRequestBuilder::fromBytes, ProtoUtils::toPrettyString
                                    )
                                    .whenComplete(withMdc((requests, throwable) -> {
                                        redisPool.release(connection);
                                    }));
                        }
                ));
    }

    public CompletableFuture<List<Map.Entry<String, IncrementCustomerAmountRequest>>> listenIncrementCustomerAmountRequestMessages() {
        return redisPool.acquire()
                .thenCompose(withMdc(connection -> {
                            return client.subscribe(
                                            connection, incrementCustomerAmountStream,
                                            incrementCustomerAmountRequestBuilder::fromBytes, ProtoUtils::toPrettyString
                                    )
                                    .whenComplete(withMdc((requests, throwable) -> {
                                        redisPool.release(connection);
                                    }));
                        }
                ));
    }

    public CompletableFuture<Void> retryDecreaseCustomerAmountRequestMessage(Map.Entry<String, DecreaseCustomerAmountRequest> entry,
                                                                             Throwable throwable) {
        return redisPool.acquire()
                .thenAccept(withMdc(connection -> {
                    var request = entry.getValue();
                    int counter = resolveCounter(request, DecreaseCustomerAmountRequest::hasRetryData, rq -> rq.getRetryData().getCounter());
                    if (counter < maxRetryCounter) {
                        var newRequest = decreaseCustomerAmountRequestBuilder.build(request, counter);
                        client.sendMessage(
                                connection, decreaseCustomerAmountStream, entry.getKey(), newRequest,
                                AbstractMessageLite::toByteArray, ProtoUtils::toPrettyString
                        );
                    } else {
                        throw new RetryExpiryException(throwable);
                    }
                }));
    }

    public CompletableFuture<Void> retryIncrementExecutorAmountRequestMessage(Map.Entry<String, IncrementExecutorAmountRequest> entry,
                                                                              Throwable throwable) {
        return redisPool.acquire()
                .thenAccept(withMdc(connection -> {
                    var request = entry.getValue();
                    int counter = resolveCounter(request, IncrementExecutorAmountRequest::hasRetryData, rq -> rq.getRetryData().getCounter());
                    if (counter < maxRetryCounter) {
                        var newRequest = incrementExecutorAmountRequestBuilder.build(request, counter);
                        client.sendMessage(
                                connection, decreaseExecutorAmountStream, entry.getKey(), newRequest,
                                AbstractMessageLite::toByteArray, ProtoUtils::toPrettyString
                        );
                    } else {
                        throw new RetryExpiryException(throwable);
                    }
                }));
    }

    public CompletableFuture<Void> retryReserveOrderRequestMessage(Map.Entry<String, ReserveOrderRequest> entry,
                                                                   Throwable throwable) {
        return redisPool.acquire()
                .thenAccept(withMdc(connection -> {
                    var request = entry.getValue();
                    int counter = resolveCounter(request, ReserveOrderRequest::hasRetryData, rq -> rq.getRetryData().getCounter());
                    if (counter < maxRetryCounter) {
                        var newRequest = reserveOrderRequestBuilder.build(request, counter);
                        client.sendMessage(
                                connection, reserveOrderStream, entry.getKey(), newRequest,
                                AbstractMessageLite::toByteArray, ProtoUtils::toPrettyString
                        );
                    } else {
                        throw new RetryExpiryException(throwable);
                    }
                }));
    }

    public CompletionStage<Void> retryCompleteOrderRequestMessage(Map.Entry<String, CompleteOrderRequest> entry,
                                                                  Throwable throwable) {
        return redisPool.acquire()
                .thenAccept(withMdc(connection -> {
                    var request = entry.getValue();
                    int counter = resolveCounter(request, CompleteOrderRequest::hasRetryData, rq -> rq.getRetryData().getCounter());
                    if (counter < maxRetryCounter) {
                        var newRequest = completeOrderRequestBuilder.build(request, counter);
                        client.sendMessage(
                                connection, completeOrderStream, entry.getKey(), newRequest,
                                AbstractMessageLite::toByteArray, ProtoUtils::toPrettyString
                        );
                    } else {
                        throw new RetryExpiryException(throwable);
                    }
                }));
    }

    public CompletionStage<Void> retryDecreaseExecutorAmountRequestMessage(Map.Entry<String, DecreaseExecutorAmountRequest> entry, Throwable throwable) {
        return redisPool.acquire()
                .thenAccept(withMdc(connection -> {
                    var request = entry.getValue();
                    int counter = resolveCounter(request, DecreaseExecutorAmountRequest::hasRetryData, rq -> rq.getRetryData().getCounter());
                    if (counter < maxRetryCounter) {
                        var newRequest = decreaseExecutorAmountRequestBuilder.build(request, counter);
                        client.sendMessage(
                                connection, decreaseExecutorAmountStream, entry.getKey(), newRequest,
                                AbstractMessageLite::toByteArray, ProtoUtils::toPrettyString
                        );
                    } else {
                        throw new RetryExpiryException(throwable);
                    }
                }));
    }

    public CompletionStage<Void> retryIncrementCustomerAmountRequestMessage(Map.Entry<String, IncrementCustomerAmountRequest> entry, Throwable throwable) {
        return redisPool.acquire()
                .thenAccept(withMdc(connection -> {
                    var request = entry.getValue();
                    int counter = resolveCounter(request, IncrementCustomerAmountRequest::hasRetryData, rq -> rq.getRetryData().getCounter());
                    if (counter < maxRetryCounter) {
                        var newRequest = incrementCustomerAmountRequestBuilder.build(request, counter);
                        client.sendMessage(
                                connection, incrementCustomerAmountStream, entry.getKey(), newRequest,
                                AbstractMessageLite::toByteArray, ProtoUtils::toPrettyString
                        );
                    } else {
                        throw new RetryExpiryException(throwable);
                    }
                }));
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
