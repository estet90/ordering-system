package ru.craftysoft.orderingsystem.user.service.dao;

import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.util.db.DbHelper;
import ru.craftysoft.orderingsystem.util.db.DbLoggerHelper;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;

import static ru.craftysoft.orderingsystem.user.error.exception.InvocationExceptionCode.DB;
import static ru.craftysoft.orderingsystem.user.error.operation.ModuleOperationCode.resolve;
import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.mapSqlException;

@Singleton
@Slf4j
public class UserDao {

    private final DbHelper dbHelper;

    @Inject
    public UserDao(DbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public List<String> getRoles(String login, String password) {
        var sql = """
                SELECT roles
                FROM users.users
                WHERE login = ? AND password = ?""";
        return DbLoggerHelper.executeWithLogging(
                log, "RoleDao.getRoles", () -> sql, () -> List.of(login, password),
                () -> {
                    try {
                        return dbHelper.selectOne(sql, resultSet -> {
                            var array = resultSet.getArray("roles");
                            if (array != null) {
                                return Arrays.asList((String[]) array.getArray());
                            }
                            return List.of();
                        }, login, password);
                    } catch (Exception e) {
                        throw mapSqlException(e, resolve(), DB);
                    }
                }
        );
    }

    public long getUserId(String login) {
        var sql = """
                SELECT id
                FROM users.users
                WHERE login = ?""";
        return DbLoggerHelper.executeWithLogging(
                log, "RoleDao.getUserId", () -> sql, () -> login,
                () -> {
                    try {
                        return dbHelper.selectOne(sql, resultSet -> resultSet.getLong("id"), login);
                    } catch (Exception e) {
                        throw mapSqlException(e, resolve(), DB);
                    }
                }
        );
    }
}
