package ru.craftysoft.orderingsystem.orderprocessing.logic.messagedriven;

import com.google.type.Money;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.craftysoft.orderingsystem.orderprocessing.DaggerTestApplicationComponent;
import ru.craftysoft.orderingsystem.orderprocessing.DaggerTestWithoutDbApplicationComponent;
import ru.craftysoft.orderingsystem.orderprocessing.OperationTest;
import ru.craftysoft.orderingsystem.orderprocessing.builder.redis.CompleteOrderRequestBuilder;
import ru.craftysoft.orderingsystem.orderprocessing.builder.redis.DecreaseExecutorAmountRequestBuilder;
import ru.craftysoft.orderingsystem.orderprocessing.extension.DbExtension;
import ru.craftysoft.orderingsystem.orderprocessing.extension.RedisExtension;
import ru.craftysoft.orderingsystem.orderprocessing.proto.CompleteOrderRequest;
import ru.craftysoft.orderingsystem.orderprocessing.proto.RetryData;
import ru.craftysoft.orderingsystem.util.db.DbHelper;
import ru.craftysoft.orderingsystem.util.proto.ProtoUtils;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.craftysoft.orderingsystem.orderprocessing.error.exception.BusinessExceptionCode.ORDER_HAS_NOT_BEEN_COMPLETED;
import static ru.craftysoft.orderingsystem.orderprocessing.error.exception.InvocationExceptionCode.DB;
import static ru.craftysoft.orderingsystem.orderprocessing.error.operation.ModuleOperationCode.COMPLETE_ORDER;
import static ru.craftysoft.orderingsystem.orderprocessing.util.StackTraceHelper.listAppender;
import static ru.craftysoft.orderingsystem.orderprocessing.util.StackTraceHelper.thenErrorStacktrace;
import static ru.craftysoft.orderingsystem.util.error.type.ExceptionType.BUSINESS;
import static ru.craftysoft.orderingsystem.util.error.type.ExceptionType.RETRYABLE;
import static ru.craftysoft.orderingsystem.util.proto.ProtoUtils.moneyToBigDecimal;

@Slf4j
public class CompleteOrderOperationTest extends OperationTest {

    @Inject
    CompleteOrderOperation operation;
    @Inject
    DbHelper dbHelper;
    @Inject
    CompleteOrderRequestBuilder completeOrderRequestBuilder;
    @Inject
    DecreaseExecutorAmountRequestBuilder decreaseExecutorAmountRequestBuilder;

    private static final long CUSTOMER_ID = 2L;
    private static final long EXECUTOR_ID = 3L;
    private static final Money AMOUNT = Money.newBuilder()
            .setUnits(100)
            .build();
    private static final Money CUSTOMER_BALANCE = Money.newBuilder()
            .setUnits(100)
            .build();
    private static final String MESSAGE_ID = "messageId";

    @Test
    @ExtendWith({
            DbExtension.class,
            RedisExtension.class,
    })
    void process() throws Exception {
        var component = DaggerTestApplicationComponent.builder().build();
        component.inject(this);
        var processingOrderId = addProcessingOrder();
        var activeOrderId = addActiveOrder();
        redisConsumerGroupInitOperation.process();
        var completeOrderRequest = givenCompleteOrderRequest(processingOrderId);
        var streamKey = propertyResolver.getStringProperty("redis.stream.complete-order.name");
        redisClient.sendMessage(streamKey, MESSAGE_ID, completeOrderRequest, CompleteOrderRequest::toByteArray, ProtoUtils::toPrettyString)
                .toCompletableFuture()
                .get();

        operation.process().toCompletableFuture().get();

        var unavailableOrderStatus = dbHelper.selectOne(connectionFactory().get(), """
                SELECT status
                FROM orders.orders
                WHERE id = ?""", resultSet -> resultSet.getString("status"), activeOrderId);
        assertEquals("unavailable", unavailableOrderStatus);
    }

    @Test
    @ExtendWith({
            DbExtension.class,
            RedisExtension.class,
    })
    void processOrderHasNotBeenCompleted() throws Exception {
        var component = DaggerTestApplicationComponent.builder().build();
        component.inject(this);
        var processingOrderId = addProcessingOrder();
        dbHelper.execute(connectionFactory().get(), "DELETE FROM orders.orders RETURNING id", resultSet -> resultSet.getLong("id"));
        redisConsumerGroupInitOperation.process();
        var completeOrderRequest = givenCompleteOrderRequest(processingOrderId);
        var streamKey = propertyResolver.getStringProperty("redis.stream.complete-order.name");
        redisClient.sendMessage(streamKey, MESSAGE_ID, completeOrderRequest, CompleteOrderRequest::toByteArray, ProtoUtils::toPrettyString)
                .toCompletableFuture()
                .get();
        var listAppender = listAppender(CompleteOrderOperation.class);

        operation.process().toCompletableFuture().get();

        var fullErrorCode = fullErrorCode(COMPLETE_ORDER, BUSINESS, ORDER_HAS_NOT_BEEN_COMPLETED);
        thenErrorStacktrace(listAppender, fullErrorCode, ORDER_HAS_NOT_BEEN_COMPLETED);
        listAppender.stop();
    }

    @Test
    @ExtendWith({
            RedisExtension.class,
    })
    void processDbNotAvailable() throws Exception {
        var component = DaggerTestWithoutDbApplicationComponent.builder().build();
        component.inject(this);
        redisConsumerGroupInitOperation.process();
        var completeOrderRequest = givenCompleteOrderRequest(1L);
        var streamKey = propertyResolver.getStringProperty("redis.stream.complete-order.name");
        redisClient.sendMessage(streamKey, MESSAGE_ID, completeOrderRequest, CompleteOrderRequest::toByteArray, ProtoUtils::toPrettyString)
                .toCompletableFuture()
                .get();
        var listAppender = listAppender(CompleteOrderOperation.class);

        operation.process().toCompletableFuture().get();

        var fullErrorCode = fullErrorCode(COMPLETE_ORDER, RETRYABLE, DB);
        thenErrorStacktrace(listAppender, fullErrorCode, DB);
        listAppender.stop();
        var entries = redisClient.subscribe(streamKey, completeOrderRequestBuilder::fromBytes, ProtoUtils::toPrettyString)
                .toCompletableFuture()
                .get();
        assertEquals(1, entries.size());
        var entry = entries.stream().iterator().next();
        assertEquals(MESSAGE_ID, entry.getKey());
        var retryableCompleteOrderRequest = entry.getValue();
        assertEquals(completeOrderRequest.getOrderId(), retryableCompleteOrderRequest.getOrderId());
        assertEquals(completeOrderRequest.getCustomerId(), retryableCompleteOrderRequest.getCustomerId());
        assertEquals(completeOrderRequest.getExecutorId(), retryableCompleteOrderRequest.getExecutorId());
        assertEquals(completeOrderRequest.getAmount(), retryableCompleteOrderRequest.getAmount());
        assertEquals(completeOrderRequest.getCustomerBalance(), retryableCompleteOrderRequest.getCustomerBalance());
        assertTrue(retryableCompleteOrderRequest.hasRetryData());
        assertEquals(1, retryableCompleteOrderRequest.getRetryData().getCounter());
    }

    @Test
    @ExtendWith({
            RedisExtension.class,
    })
    void processRetryExpiry() throws Exception {
        var component = DaggerTestWithoutDbApplicationComponent.builder().build();
        component.inject(this);
        redisConsumerGroupInitOperation.process();
        var completeOrderRequest = CompleteOrderRequest.newBuilder(givenCompleteOrderRequest(1L))
                .setRetryData(RetryData.newBuilder()
                        .setCounter(propertyResolver.getIntProperty("redis.max-retry-counter"))
                        .build())
                .build();
        var streamKey = propertyResolver.getStringProperty("redis.stream.complete-order.name");
        redisClient.sendMessage(streamKey, MESSAGE_ID, completeOrderRequest, CompleteOrderRequest::toByteArray, ProtoUtils::toPrettyString)
                .toCompletableFuture()
                .get();
        var listAppender = listAppender(CompleteOrderOperation.class);

        operation.process().toCompletableFuture().get();

        var fullErrorCode = fullErrorCode(COMPLETE_ORDER, RETRYABLE, DB);
        thenErrorStacktrace(listAppender, fullErrorCode, DB);
        listAppender.stop();
        var responseStreamKey = propertyResolver.getStringProperty("redis.stream.decrease-executor-amount.name");
        var entries = redisClient.subscribe(responseStreamKey, decreaseExecutorAmountRequestBuilder::fromBytes, ProtoUtils::toPrettyString)
                .toCompletableFuture()
                .get();
        assertEquals(1, entries.size());
        var entry = entries.stream().iterator().next();
        assertEquals(MESSAGE_ID, entry.getKey());
        var decreaseExecutorAmountRequest = entry.getValue();
        assertEquals(completeOrderRequest.getOrderId(), decreaseExecutorAmountRequest.getOrderId());
        assertEquals(completeOrderRequest.getCustomerId(), decreaseExecutorAmountRequest.getCustomerId());
        assertEquals(completeOrderRequest.getExecutorId(), decreaseExecutorAmountRequest.getExecutorId());
        assertEquals(completeOrderRequest.getAmount(), decreaseExecutorAmountRequest.getAmount());
    }

    private CompleteOrderRequest givenCompleteOrderRequest(long orderId) {
        return CompleteOrderRequest.newBuilder()
                .setOrderId(orderId)
                .setCustomerId(CUSTOMER_ID)
                .setExecutorId(EXECUTOR_ID)
                .setAmount(AMOUNT)
                .setCustomerBalance(CUSTOMER_BALANCE)
                .build();
    }

    @SneakyThrows
    private long addProcessingOrder() {
        var sql = """
                INSERT INTO orders.orders (name, price, customer_id, executor_id, status)
                VALUES ('test order10', ?, ?, ?, 'in_processing')
                RETURNING id""";
        return (long) dbHelper.insert(connectionFactory().get(), sql, moneyToBigDecimal(AMOUNT), CUSTOMER_ID, EXECUTOR_ID);
    }

    @SneakyThrows
    private long addActiveOrder() {
        var sql = """
                INSERT INTO orders.orders (name, price, customer_id, executor_id, status)
                VALUES ('test order20', ?, ?, null, 'active')
                RETURNING id""";
        return (long) dbHelper.insert(connectionFactory().get(), sql, moneyToBigDecimal(AMOUNT).add(moneyToBigDecimal(CUSTOMER_BALANCE)), CUSTOMER_ID);
    }
}