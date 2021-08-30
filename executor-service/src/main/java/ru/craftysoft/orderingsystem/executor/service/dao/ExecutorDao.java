package ru.craftysoft.orderingsystem.executor.service.dao;

import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.executor.dto.Executor;
import ru.craftysoft.orderingsystem.util.db.DbHelper;
import ru.craftysoft.orderingsystem.util.db.DbLoggerHelper;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.util.List;

import static ru.craftysoft.orderingsystem.executor.error.exception.InvocationExceptionCode.DB;
import static ru.craftysoft.orderingsystem.executor.error.operation.ModuleOperationCode.resolve;
import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.mapSqlException;

@Singleton
@Slf4j
public class ExecutorDao {

    private final DbHelper dbHelper;

    @Inject
    public ExecutorDao(DbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public Executor getExecutorByUserId(long userId) {
        var sql = """
                SELECT id, balance
                FROM executors.executors
                WHERE user_id = ?""";
        return DbLoggerHelper.executeWithLogging(
                log, "ExecutorDao.getExecutorByUserId", () -> sql, () -> userId,
                () -> {
                    try {
                        return dbHelper.selectOne(sql, resultSet -> new Executor(
                                resultSet.getLong("id"),
                                resultSet.getBigDecimal("balance")
                        ), userId);
                    } catch (Exception e) {
                        throw mapSqlException(e, resolve(), DB);
                    }
                });
    }

    public Executor getExecutorById(long id) {
        var sql = """
                SELECT balance
                FROM executors.executors
                WHERE id = ?""";
        return DbLoggerHelper.executeWithLogging(
                log, "ExecutorDao.getExecutorById", () -> sql, () -> id,
                () -> {
                    try {
                        return dbHelper.selectOne(sql, resultSet -> new Executor(
                                id,
                                resultSet.getBigDecimal("balance")
                        ), id);
                    } catch (Exception e) {
                        throw mapSqlException(e, resolve(), DB);
                    }
                });
    }

    public int incrementAmount(long id, BigDecimal amount) {
        var sql = """
                UPDATE executors.executors
                SET balance = (SELECT balance FROM executors.executors WHERE id = ?) + ?
                WHERE id = ?""";
        return DbLoggerHelper.executeWithLogging(
                log, "ExecutorDao.incrementAmount", () -> sql, () -> List.of(id, amount, id),
                () -> {
                    try {
                        return dbHelper.update(sql, id, amount, id);
                    } catch (Exception e) {
                        throw mapSqlException(e, resolve(), DB);
                    }
                }
        );
    }

    public int decreaseAmount(long id, BigDecimal amount) {
        var sql = """
                UPDATE executors.executors
                SET balance = (SELECT balance FROM executors.executors WHERE id = ?) - ?
                WHERE id = ? AND balance >= ?""";
        return DbLoggerHelper.executeWithLogging(
                log, "ExecutorDao.decreaseAmount", () -> sql, () -> List.of(id, amount, id, amount),
                () -> {
                    try {
                        return dbHelper.update(sql, id, amount, id, amount);
                    } catch (Exception e) {
                        throw mapSqlException(e, resolve(), DB);
                    }
                }
        );
    }
}
