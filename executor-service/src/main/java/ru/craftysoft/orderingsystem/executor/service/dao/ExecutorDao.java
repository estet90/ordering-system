package ru.craftysoft.orderingsystem.executor.service.dao;

import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.executor.dto.Executor;
import ru.craftysoft.orderingsystem.util.db.DbHelper;
import ru.craftysoft.orderingsystem.util.db.DbLoggerHelper;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.util.List;

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
                () -> dbHelper.selectOne(sql, resultSet -> new Executor(
                        resultSet.getLong("id"),
                        resultSet.getBigDecimal("balance")
                ), userId));
    }

    public Executor getExecutorById(long id) {
        var sql = """
                SELECT balance
                FROM executors.executors
                WHERE id = ?""";
        return DbLoggerHelper.executeWithLogging(
                log, "ExecutorDao.getExecutorById", () -> sql, () -> id,
                () -> dbHelper.selectOne(sql, resultSet -> new Executor(
                        id,
                        resultSet.getBigDecimal("balance")
                ), id));
    }

    public int incrementAmount(long id, BigDecimal amount) {
        var sql = """
                UPDATE executors.executors
                SET balance = (SELECT balance FROM executors.executors WHERE id = ?) + ?
                WHERE id = ?""";
        return DbLoggerHelper.executeWithLogging(
                log, "ExecutorDao.incrementAmount", () -> sql, () -> List.of(id, amount, id),
                () -> dbHelper.update(sql, id, amount, id)
        );
    }

    public int decreaseAmount(long id, BigDecimal amount) {
        var sql = """
                UPDATE executors.executors
                SET balance = (SELECT balance FROM executors.executors WHERE id = ?) - ?
                WHERE id = ? AND balance >= ?""";
        return DbLoggerHelper.executeWithLogging(
                log, "ExecutorDao.decreaseAmount", () -> sql, () -> List.of(id, amount, id, amount),
                () -> dbHelper.update(sql, id, amount, id, amount)
        );
    }
}
