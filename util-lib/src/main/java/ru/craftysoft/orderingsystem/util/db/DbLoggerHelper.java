package ru.craftysoft.orderingsystem.util.db;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.MDC;
import ru.craftysoft.orderingsystem.util.uuid.UuidUtils;

import java.util.List;
import java.util.function.Supplier;

import static java.util.Objects.isNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DbLoggerHelper {

    /**
     * Выполнение запросов с логированием при уровнях логирования DEBUG/TRACE
     *
     * @param logger             логгер, который будет вести запись
     * @param point              шаблон для логирования
     * @param sqlSupplier        коллбэк, возвращающий строку запроса к БД
     * @param parametersSupplier коллбэк, возвращающий параметры запроса
     * @param query              коллбэк, возвращающий результат запроса
     * @param <T>                тип результата
     * @return результат запроса
     */
    public static <T> T executeWithLogging(Logger logger,
                                           String point,
                                           Supplier<String> sqlSupplier,
                                           Supplier<Object> parametersSupplier,
                                           Supplier<T> query) {
        MDC.put("queryId", UuidUtils.generateDefaultUuid());
        try {
            logIn(point, logger, sqlSupplier.get(), parametersSupplier.get());
            var result = query.get();
            logOut(point, logger, result);
            return result;
        } catch (Exception e) {
            logger.error("{}.thrown {}", point, e.getMessage());
            throw e;
        } finally {
            MDC.remove("queryId");
        }
    }

    private static void logIn(String point, Logger logger, String query, Object... parameters) {
        if (logger.isTraceEnabled()) {
            if (isNull(parameters)) {
                logger.trace("{}.in\nзапрос без параметров: {}", point, query);
            } else {
                logger.trace("{}.in\nзапрос: {}\nпараметры:{}", point, query, parameters);
            }
        } else if (logger.isDebugEnabled()) {
            logger.debug("{}.in", point);
        }
    }

    private static <T> void logOut(String point, Logger logger, T result) {
        if (logger.isTraceEnabled()) {
            logger.trace("{}.out результат: {}", point, result);
        } else if (logger.isDebugEnabled()) {
            if (result instanceof List<?> list) {
                logger.debug("{}.out количество строк: {}", point, list.size());
            } else {
                logger.debug("{}.out", point);
            }
        }
    }

}
