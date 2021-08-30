package ru.craftysoft.orderingsystem.orderprocessing.util;

import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class DbHelper {

    public static void executeQueryFromClasspath(Supplier<Connection> connectionFactory, String path) {
        try (var inputStream = DbHelper.class.getClassLoader().getResourceAsStream(path);
             var reader = new InputStreamReader(Objects.requireNonNull(inputStream), StandardCharsets.UTF_8);
             var bufferedReader = new BufferedReader(reader)) {
            var sql = bufferedReader.lines()
                    .collect(Collectors.joining("\n"));
            executeQueries(connectionFactory, sql);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void executeQueries(Supplier<Connection> connectionFactory, String... queries) {
        for (var query : queries) {
            try (var connection = connectionFactory.get();
                 var statement = connection.createStatement()) {
                statement.execute(query);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static List<Object> executeQueryWithReturning(Supplier<Connection> connectionFactory, String... queries) {
        List<Object> idList = new ArrayList<>();
        for (var query : queries) {
            try (var connection = connectionFactory.get();
                 var preparedStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                preparedStatement.executeUpdate();
                try (var resultSet = preparedStatement.getGeneratedKeys()) {
                    if (resultSet.next()) {
                        idList.add(resultSet.getObject("id"));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return idList;
    }


    @SneakyThrows
    public static Connection getConnection(String url, String username, String password) {
        Class.forName("org.postgresql.Driver");
        return DriverManager.getConnection(url, username, password);
    }

}
