package ru.craftysoft.orderingsystem.orderprocessing.logic;

import lombok.SneakyThrows;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.craftysoft.orderingsystem.orderprocessing.DaggerTestApplicationComponent;
import ru.craftysoft.orderingsystem.orderprocessing.OperationTest;
import ru.craftysoft.orderingsystem.orderprocessing.dto.TestOrder;
import ru.craftysoft.orderingsystem.orderprocessing.extension.DbExtension;
import ru.craftysoft.orderingsystem.orderprocessing.extension.RedisExtension;
import ru.craftysoft.orderingsystem.orderprocessing.proto.DecreaseExecutorAmountRequest;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.craftysoft.orderingsystem.util.proto.ProtoUtils.moneyToBigDecimal;

public class ExtractOrderOperationTest extends OperationTest {

    @Inject
    ExtractOrderOperation operation;

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
        assertThat(order.status()).isEqualTo("in_processing");
        var message = thenSentMessage("redis.stream.decrease-customer-amount.name");
        var decreaseCustomerAmountRequest = DecreaseExecutorAmountRequest.parseFrom(message.getBody().get("payload").getBytes(StandardCharsets.UTF_8));
        assertThat(decreaseCustomerAmountRequest.hasRetryData()).isFalse();
        assertThat(decreaseCustomerAmountRequest.getOrderId()).isEqualTo(order.id());
        assertThat(decreaseCustomerAmountRequest.getCustomerId()).isEqualTo(order.customerId());
        assertThat(decreaseCustomerAmountRequest.getAmount()).isNotNull().is(new Condition<>(amount -> {
            var amountAsBigDecimal = moneyToBigDecimal(amount);
            assertThat(amountAsBigDecimal).isEqualTo(order.price());
            return true;
        }, "price"));
    }

    @Test
    @ExtendWith({
            DbExtension.class,
    })
    void processSendError() throws Exception {
        var component = DaggerTestApplicationComponent.builder().build();
        component.inject(this);
        var reservedOrderId = reserveOrder();

        assertThrows(Exception.class, () -> operation.process().get());

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
        assertThat(order.status()).isEqualTo("reserved");
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