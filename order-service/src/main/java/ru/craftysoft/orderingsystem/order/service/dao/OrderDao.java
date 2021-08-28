package ru.craftysoft.orderingsystem.order.service.dao;

import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.order.dto.AddedOrder;
import ru.craftysoft.orderingsystem.order.dto.Order;
import ru.craftysoft.orderingsystem.util.db.DbHelper;
import ru.craftysoft.orderingsystem.util.db.DbLoggerHelper;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
@Slf4j
public class OrderDao {

    private final DbHelper dbHelper;

    @Inject
    public OrderDao(DbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public List<Order> getOrders(String status) {
        var sql = """
                SELECT id, name, price, customer_id
                FROM orders.orders
                WHERE status = ?::orders.order_status""";
        return DbLoggerHelper.executeWithLogging(
                log, "OrderDao.getOrders", () -> sql, () -> status,
                () -> dbHelper.select(sql, resultSet -> new Order(
                        resultSet.getLong("id"),
                        resultSet.getString("name"),
                        resultSet.getBigDecimal("price"),
                        resultSet.getLong("customer_id")
                ), status)
        );
    }

    public long addOrder(AddedOrder order) {
        var sql = """
                INSERT INTO orders.orders (name, price, customer_id, status)
                VALUES (?, ?, ?, ?::orders.order_status)""";
        return DbLoggerHelper.executeWithLogging(
                log, "OrderDao.getOrders", () -> sql, () -> order,
                () -> (long) dbHelper.insert(sql, order.name(), order.price(), order.customerId(), order.status())
        );
    }

    public int updateOrderStatus(long id,
                                 long executorId,
                                 @Nonnull String oldStatus,
                                 @Nonnull String newStatus) {
        var sql = """
                UPDATE orders.orders
                SET status = ?::orders.order_status,
                    executor_id = ?
                WHERE id = ? AND status = ?::orders.order_status""";
        return DbLoggerHelper.executeWithLogging(
                log, "OrderDao.getOrders", () -> sql, () -> List.of(newStatus, executorId, id, oldStatus),
                () -> dbHelper.update(sql, newStatus, executorId, id, oldStatus)
        );
    }
}
