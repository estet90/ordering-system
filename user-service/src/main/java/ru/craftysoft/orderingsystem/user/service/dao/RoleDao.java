package ru.craftysoft.orderingsystem.user.service.dao;

import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.util.db.DbHelper;
import ru.craftysoft.orderingsystem.util.db.DbLoggerHelper;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;

@Singleton
@Slf4j
public class RoleDao {

    private final DbHelper dbHelper;

    @Inject
    public RoleDao(DbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public List<String> getRoles(String login, String password) {
        var sql = """
                SELECT roles FROM users.users
                WHERE login = ? AND password = ?""";
        return DbLoggerHelper.executeWithLogging(
                log, "RoleDao.getRoles", () -> sql, () -> List.of(login, password),
                () -> dbHelper.selectOne(sql, resultSet -> {
                    var array = resultSet.getArray("roles");
                    if (array != null) {
                        return Arrays.asList((String[]) array.getArray());
                    }
                    return List.of();
                }, login, password)
        );
    }
}
