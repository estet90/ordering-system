package ru.craftysoft.orderingsystem.util.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class DbHelper {

    private final DataSource dataSource;

    public <T> List<T> select(String sql, ResultSetExtractor<T> resultSetExtractor, Object... parameters) throws SQLException {
        try (var connection = dataSource.getConnection()) {
            return select(connection, sql, resultSetExtractor, parameters);
        }
    }

    public <T> T selectOne(String sql, ResultSetExtractor<T> resultSetExtractor, Object... parameters) throws SQLException {
        try (var connection = dataSource.getConnection()) {
            return selectOne(connection, sql, resultSetExtractor, parameters);
        }
    }

    public <T> List<T> select(Connection connection, String sql, ResultSetExtractor<T> resultSetExtractor, Object... parameters) throws SQLException {
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
        }
    }

    public <T> T selectOne(Connection connection, String sql, ResultSetExtractor<T> resultSetExtractor, Object... parameters) throws SQLException {
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
        }
    }

    public Object insert(String sql, Object... parameters) throws SQLException {
        try (var connection = dataSource.getConnection()) {
            return insert(connection, sql, parameters);
        }
    }

    public Object insert(Connection connection, String sql, Object... parameters) throws SQLException {
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
        }
    }

    public int update(String sql, Object... parameters) throws SQLException {
        try (var connection = dataSource.getConnection()) {
            return update(connection, sql, parameters);
        }
    }

    public int update(Connection connection, String sql, Object... parameters) throws SQLException {
        try (var preparedStatement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                preparedStatement.setObject(i + 1, parameters[i]);
            }
            return preparedStatement.executeUpdate();
        }
    }

    public <T> List<T> execute(String sql, ResultSetExtractor<T> resultSetExtractor, Object... parameters) throws SQLException {
        try (var connection = dataSource.getConnection()) {
            return execute(connection, sql, resultSetExtractor, parameters);
        }
    }

    public <T> List<T> execute(Connection connection, String sql, ResultSetExtractor<T> resultSetExtractor, Object... parameters) throws SQLException {
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
        }
    }

    public <T> T executeOne(String sql, ResultSetExtractor<T> resultSetExtractor, Object... parameters) throws SQLException {
        try (var connection = dataSource.getConnection()) {
            return executeOne(connection, sql, resultSetExtractor, parameters);
        }
    }

    public <T> T executeOne(Connection connection, String sql, ResultSetExtractor<T> resultSetExtractor, Object... parameters) throws SQLException {
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
        }
    }

}
