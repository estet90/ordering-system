package ru.craftysoft.orderingsystem.util.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
public class DbHelper {

    private final DataSource dataSource;

    /**
     * Выбор нескольких строк с маппингом.
     *
     * @param sql                запрос
     * @param resultSetExtractor маппинг
     * @param parameters         параметры запроса
     * @param <T>                тип, в который будет преобразована строка запроса
     * @return список
     */
    public <T> List<T> select(String sql, ResultSetExtractor<T> resultSetExtractor, Object... parameters) {
        try (var connection = dataSource.getConnection()) {
            return select(connection, sql, resultSetExtractor, parameters);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Выбор одной строки с маппингом.
     *
     * @param sql                запрос
     * @param resultSetExtractor маппинг
     * @param parameters         параметры запроса
     * @param <T>                тип, в который будет преобразована строка запроса
     * @return список
     */
    public <T> T selectOne(String sql, ResultSetExtractor<T> resultSetExtractor, Object... parameters) {
        try (var connection = dataSource.getConnection()) {
            return selectOne(connection, sql, resultSetExtractor, parameters);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Выбор нескольких строк с маппингом и переданным соединением. При использовании данного метода соединение необходимо закрывать вручную.
     *
     * @param connection         используемое соединение
     * @param sql                запрос
     * @param resultSetExtractor маппинг
     * @param parameters         параметры запроса
     * @param <T>                тип, в который будет преобразована строка запроса
     * @return список
     */
    public <T> List<T> select(Connection connection, String sql, ResultSetExtractor<T> resultSetExtractor, Object... parameters) {
        try (var preparedStatement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                preparedStatement.setObject(i + 1, parameters[i]);
            }
            try (var resultSet = preparedStatement.executeQuery()) {
                var result = new ArrayList<T>();
                while (resultSet.next()) {
                    var row = resultSetExtractor.extract(resultSet);
                    result.add(row);
                }
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Выбор одной строки с маппингом и переданным соединением. При использовании данного метода соединение необходимо закрывать вручную.
     *
     * @param connection         используемое соединение
     * @param sql                запрос
     * @param resultSetExtractor маппинг
     * @param parameters         параметры запроса
     * @param <T>                тип, в который будет преобразована строка запроса
     * @return список
     */
    public <T> T selectOne(Connection connection, String sql, ResultSetExtractor<T> resultSetExtractor, Object... parameters) {
        try (var preparedStatement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                preparedStatement.setObject(i + 1, parameters[i]);
            }
            try (var resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSetExtractor.extract(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Вставка записи.
     *
     * @param sql        запрос
     * @param parameters параметры запроса
     * @return значение сгенерированного первичного ключа
     */
    public Object insert(String sql, Object... parameters) {
        try (var connection = dataSource.getConnection()) {
            return insert(connection, sql, parameters);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Вставка записи с переданным соединением. При использовании данного метода соединение необходимо закрывать вручную.
     *
     * @param connection используемое соединение
     * @param sql        запрос
     * @param parameters параметры запроса
     * @return значение сгенерированного первичного ключа
     */
    public Object insert(Connection connection, String sql, Object... parameters) {
        try (var preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < parameters.length; i++) {
                preparedStatement.setObject(i + 1, parameters[i]);
            }
            preparedStatement.executeUpdate();
            try (var resultSet = preparedStatement.getGeneratedKeys()) {
                if (resultSet.next()) {
                    return resultSet.getObject(1);
                }
                throw new RuntimeException();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Обновление записи.
     *
     * @param sql        запрос
     * @param parameters параметры запроса
     * @return количество обновлённых записей
     */
    public int update(String sql, Object... parameters) {
        try (var connection = dataSource.getConnection()) {
            return update(connection, sql, parameters);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Обновление записей переданным соединением. При использовании данного метода соединение необходимо закрывать вручную.
     *
     * @param connection используемое соединение
     * @param sql        запрос
     * @param parameters параметры запроса
     * @return количество обновлённых записей
     */
    public int update(Connection connection, String sql, Object... parameters) {
        try (var preparedStatement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                preparedStatement.setObject(i + 1, parameters[i]);
            }
            return preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Обновление записей и получение списка.
     *
     * @param sql                запрос
     * @param resultSetExtractor маппинг
     * @param parameters         параметры запроса
     * @param <T>                тип, в который будет преобразована строка запроса
     * @return список
     */
    public <T> List<T> execute(String sql, ResultSetExtractor<T> resultSetExtractor, Object... parameters) {
        try (var connection = dataSource.getConnection()) {
            return execute(connection, sql, resultSetExtractor, parameters);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Обновление записей и получение списка с полученным соединением.
     *
     * @param connection         используемое соединение
     * @param sql                запрос
     * @param resultSetExtractor маппинг
     * @param parameters         параметры запроса
     * @param <T>                тип, в который будет преобразована строка запроса
     * @return список
     */
    public <T> List<T> execute(Connection connection, String sql, ResultSetExtractor<T> resultSetExtractor, Object... parameters) {
        try (var preparedStatement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                preparedStatement.setObject(i + 1, parameters[i]);
            }
            preparedStatement.execute();
            try (var resultSet = preparedStatement.getResultSet()) {
                var result = new ArrayList<T>();
                while (resultSet.next()) {
                    var row = resultSetExtractor.extract(resultSet);
                    result.add(row);
                }
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Обновление записи и получение единичного значения.
     *
     * @param sql                запрос
     * @param resultSetExtractor маппинг
     * @param parameters         параметры запроса
     * @param <T>                тип, в который будет преобразована строка запроса
     * @return список
     */
    public <T> T executeOne(String sql, ResultSetExtractor<T> resultSetExtractor, Object... parameters) {
        try (var connection = dataSource.getConnection()) {
            return executeOne(connection, sql, resultSetExtractor, parameters);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Обновление записей и получение единичного значения с полученным соединением.
     *
     * @param connection         используемое соединение
     * @param sql                запрос
     * @param resultSetExtractor маппинг
     * @param parameters         параметры запроса
     * @param <T>                тип, в который будет преобразована строка запроса
     * @return список
     */
    public <T> T executeOne(Connection connection, String sql, ResultSetExtractor<T> resultSetExtractor, Object... parameters) {
        try (var preparedStatement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                preparedStatement.setObject(i + 1, parameters[i]);
            }
            preparedStatement.execute();
            try (var resultSet = preparedStatement.getResultSet()) {
                if (resultSet.next()) {
                    return resultSetExtractor.extract(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Выполнение нескольких запросов в одной транзакции.
     *
     * @param query коллбэк, принимающий на вход соединение и возвращающий какой-то результат
     * @param <T>   тип возвращаемого результата
     * @return результат функции
     */
    public <T> T inTransaction(Function<Connection, T> query) {
        try (var connection = dataSource.getConnection()) {
            try {
                connection.setAutoCommit(false);
                var result = query.apply(connection);
                connection.commit();
                return result;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception e) {
            log.error("DbHelper.inTransaction.thrown {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            MDC.remove("transactionId");
        }
    }

}
