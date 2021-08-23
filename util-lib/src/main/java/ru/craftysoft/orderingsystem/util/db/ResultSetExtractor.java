package ru.craftysoft.orderingsystem.util.db;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Обёртка для функции извлечения данных из ResultSet, позволяющая не обрабатывать явно {@link SQLException}
 *
 * @param <T>
 */
public interface ResultSetExtractor<T> {

    T extract(ResultSet resultSet) throws SQLException;

}
