package ru.craftysoft.orderingsystem.orderprocessing.logic;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.craftysoft.orderingsystem.orderprocessing.DaggerTestApplicationComponent;
import ru.craftysoft.orderingsystem.orderprocessing.DaggerTestWithoutDbApplicationComponent;
import ru.craftysoft.orderingsystem.orderprocessing.OperationTest;
import ru.craftysoft.orderingsystem.orderprocessing.builder.redis.DecreaseCustomerAmountRequestBuilder;
import ru.craftysoft.orderingsystem.orderprocessing.dto.TestOrder;
import ru.craftysoft.orderingsystem.orderprocessing.extension.DbExtension;
import ru.craftysoft.orderingsystem.orderprocessing.extension.RedisExtension;
import ru.craftysoft.orderingsystem.util.db.DbHelper;
import ru.craftysoft.orderingsystem.util.error.exception.RetryableException;
import ru.craftysoft.orderingsystem.util.proto.ProtoUtils;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;
import static ru.craftysoft.orderingsystem.orderprocessing.error.exception.InvocationExceptionCode.DB;
import static ru.craftysoft.orderingsystem.orderprocessing.error.exception.InvocationExceptionCode.REDIS_SEND;
import static ru.craftysoft.orderingsystem.orderprocessing.error.operation.ModuleOperationCode.EXTRACT_ORDER;
import static ru.craftysoft.orderingsystem.orderprocessing.util.StackTraceHelper.listAppender;
import static ru.craftysoft.orderingsystem.orderprocessing.util.StackTraceHelper.thenErrorStacktrace;
import static ru.craftysoft.orderingsystem.util.error.type.ExceptionType.RETRYABLE;
import static ru.craftysoft.orderingsystem.util.proto.ProtoUtils.moneyToBigDecimal;

public class ExtractOrderOperationTest extends OperationTest {

    @Inject
    ExtractOrderOperation operation;
    @Inject
    DbHelper dbHelper;
    @Inject
    DecreaseCustomerAmountRequestBuilder decreaseCustomerAmountRequestBuilder;

    @Test
    @ExtendWith({
            DbExtension.class,
            RedisExtension.class,
    })
    void process() throws Exception {
        var component = DaggerTestApplicationComponent.builder().build();
        component.inject(this);
        var reservedOrderId = reserveOrder();
        redisConsumerGroupInitOperation.process();

        operation.process().get();

        var order = dbHelper.selectOne(
                connectionFactory().get(),
                """
                        SELECT id, status, price, customer_id, executor_id
                        FROM orders.orders
                        WHERE id = ?""",
                resultSet -> new TestOrder(
                        resultSet.getLong("id"),
                        resultSet.getString("status"),
                        resultSet.getBigDecimal("price"),
                        resultSet.getLong("customer_id"),
                        resultSet.getLong("executor_id")
                ),
                reservedOrderId
        );
        assertEquals("in_processing", order.status());
        var streamKey = propertyResolver.getStringProperty("redis.stream.decrease-customer-amount.name");
        var entries = redisClient.subscribe(streamKey, decreaseCustomerAmountRequestBuilder::fromBytes, ProtoUtils::toPrettyString)
                .toCompletableFuture()
                .get();
        assertEquals(1, entries.size());
        var decreaseCustomerAmountRequest = entries.get(0).getValue();
        assertFalse(decreaseCustomerAmountRequest.hasRetryData());
        assertEquals(order.id(), decreaseCustomerAmountRequest.getOrderId());
        assertEquals(order.customerId(), decreaseCustomerAmountRequest.getCustomerId());
        var amount = decreaseCustomerAmountRequest.getAmount();
        assertNotNull(amount);
        var amountAsBigDecimal = moneyToBigDecimal(amount);
        assertEquals(order.price(), amountAsBigDecimal);
    }

    @Test
    @ExtendWith({
            DbExtension.class,
    })
    void processSendError() throws Exception {
        var component = DaggerTestApplicationComponent.builder().build();
        component.inject(this);
        var reservedOrderId = reserveOrder();
        var listAppender = listAppender(ExtractOrderOperation.class);

        operation.process().get();

        var fullErrorCode = fullErrorCode(EXTRACT_ORDER, RETRYABLE, REDIS_SEND);
        thenErrorStacktrace(listAppender, fullErrorCode, REDIS_SEND);
        listAppender.stop();
        var order = dbHelper.selectOne(
                connectionFactory().get(),
                """
                        SELECT id, status, price, customer_id, executor_id
                        FROM orders.orders
                        WHERE id = ?""",
                resultSet -> new TestOrder(
                        resultSet.getLong("id"),
                        resultSet.getString("status"),
                        resultSet.getBigDecimal("price"),
                        resultSet.getLong("customer_id"),
                        resultSet.getLong("executor_id")
                ),
                reservedOrderId
        );
        assertEquals("reserved", order.status());
    }

    @Test
    void processExtractError() {
        var component = DaggerTestWithoutDbApplicationComponent.builder().build();
        component.inject(this);

        var exception = assertThrows(RetryableException.class, () -> operation.process().get());

        var fullErrorCode = fullErrorCode(EXTRACT_ORDER, RETRYABLE, DB);
        assertEquals(fullErrorCode, exception.getFullErrorCode());
    }

    @SneakyThrows
    protected long reserveOrder() {
        var sql = """
                UPDATE orders.orders
                SET status = 'reserved'::orders.order_status,
                    executor_id =
                      (SELECT id FROM executors.executors WHERE user_id = (SELECT id FROM users.users WHERE login = ?))
                WHERE customer_id =
                      (SELECT id FROM customers.customers WHERE user_id = (SELECT id FROM users.users WHERE login = ?))
                RETURNING id""";
        return dbHelper.executeOne(connectionFactory().get(), sql, resultSet -> resultSet.getLong("id"), "executor1", "customer1");
    }

}