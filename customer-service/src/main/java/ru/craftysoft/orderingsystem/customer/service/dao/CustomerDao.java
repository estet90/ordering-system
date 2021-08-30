package ru.craftysoft.orderingsystem.customer.service.dao;

import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.customer.dto.Customer;
import ru.craftysoft.orderingsystem.util.db.DbHelper;
import ru.craftysoft.orderingsystem.util.db.DbLoggerHelper;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.util.List;

import static ru.craftysoft.orderingsystem.customer.error.exception.InvocationExceptionCode.DB;
import static ru.craftysoft.orderingsystem.customer.error.operation.ModuleOperationCode.resolve;
import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.mapSqlException;

@Singleton
@Slf4j
public class CustomerDao {

    private final DbHelper dbHelper;

    @Inject
    public CustomerDao(DbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public Customer getCustomerByUserId(long userId) {
        var sql = """
                SELECT id, balance
                FROM customers.customers
                WHERE user_id = ?""";
        return DbLoggerHelper.executeWithLogging(
                log, "CustomerDao.getCustomerByUserId", () -> sql, () -> userId,
                () -> {
                    try {
                        return dbHelper.selectOne(sql, resultSet -> new Customer(
                                resultSet.getLong("id"),
                                resultSet.getBigDecimal("balance")
                        ), userId);
                    } catch (Exception e) {
                        throw mapSqlException(e, resolve(), DB);
                    }
                });
    }

    public Customer getCustomerById(long id) {
        var sql = """
                SELECT balance
                FROM customers.customers
                WHERE id = ?""";
        return DbLoggerHelper.executeWithLogging(
                log, "CustomerDao.getCustomerById", () -> sql, () -> id,
                () -> {
                    try {
                        return dbHelper.selectOne(sql, resultSet -> new Customer(
                                id,
                                resultSet.getBigDecimal("balance")
                        ), id);
                    } catch (Exception e) {
                        throw mapSqlException(e, resolve(), DB);
                    }
                });
    }

    public BigDecimal incrementAmount(long id, BigDecimal amount) {
        var sql = """
                UPDATE customers.customers
                SET balance = (SELECT balance FROM customers.customers WHERE id = ?) + ?
                WHERE id = ?
                RETURNING balance""";
        return DbLoggerHelper.executeWithLogging(
                log, "CustomerDao.getCustomerById", () -> sql, () -> List.of(id, amount, id),
                () -> {
                    try {
                        return dbHelper.executeOne(sql, resultSet -> resultSet.getBigDecimal("balance"), id, amount, id);
                    } catch (Exception e) {
                        throw mapSqlException(e, resolve(), DB);
                    }
                }
        );
    }

    public BigDecimal decreaseAmount(long id, BigDecimal amount) {
        var sql = """
                UPDATE customers.customers
                SET balance = (SELECT balance FROM customers.customers WHERE id = ?) - ?
                WHERE id = ? AND balance >= ?
                RETURNING balance""";
        return DbLoggerHelper.executeWithLogging(
                log, "CustomerDao.getCustomerById", () -> sql, () -> List.of(id, amount, id, amount),
                () -> {
                    try {
                        return dbHelper.executeOne(sql, resultSet -> resultSet.getBigDecimal("balance"), id, amount, id, amount);
                    } catch (Exception e) {
                        throw mapSqlException(e, resolve(), DB);
                    }
                }
        );
    }

}
