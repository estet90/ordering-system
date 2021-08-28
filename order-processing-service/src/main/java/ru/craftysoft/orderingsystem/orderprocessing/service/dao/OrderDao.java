package ru.craftysoft.orderingsystem.orderprocessing.service.dao;

import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.orderprocessing.dto.Order;
import ru.craftysoft.orderingsystem.util.db.DbHelper;
import ru.craftysoft.orderingsystem.util.db.DbLoggerHelper;
import ru.craftysoft.orderingsystem.util.db.ResultSetExtractor;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Singleton
@Slf4j
public class OrderDao {

    private final DbHelper dbHelper;

    @Inject
    public OrderDao(DbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public List<Order> processOrders(String oldStatus, String newStatus, int limit) {
        var sql = """
                UPDATE orders.orders
                SET status = ?::orders.order_status
                WHERE id IN (SELECT id
                             FROM orders.orders
                             WHERE status = ?::orders.order_status
                             ORDER BY id
                             LIMIT ? OFFSET 0)
                RETURNING id, price, customer_id, executor_id""";
        return DbLoggerHelper.executeWithLogging(
                log, "OrderDao.processOrders", () -> sql, () -> List.of(newStatus, oldStatus, limit),
                () -> dbHelper.execute(sql, resultSet -> new Order(
                        resultSet.getLong("id"),
                        resultSet.getBigDecimal("price"),
                        resultSet.getLong("customer_id"),
                        resultSet.getLong("executor_id")
                ), newStatus, oldStatus, limit)
        );
    }

    public int updateOrderStatus(long id, String status) {
        var sql = """
                UPDATE orders.orders
                SET status = ?::orders.order_status
                WHERE id = ?""";
        return DbLoggerHelper.executeWithLogging(
                log, "OrderDao.updateOrderStatus", () -> sql, () -> List.of(status, id),
                () -> dbHelper.update(sql, status, id)
        );
    }

    public int completeOrder(long id, long customerId, BigDecimal customerBalance) {
        var sql = """
                SELECT complete_order
                FROM orders.complete_order(?, ?, ?)""";
        return DbLoggerHelper.executeWithLogging(
                log, "OrderDao.completeOrder", () -> sql, () -> List.of(id, customerId, customerBalance),
                () -> dbHelper.executeOne(sql, resultSet -> resultSet.getInt("complete_order"), id, customerId, customerBalance)
        );
    }
}
