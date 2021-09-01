package ru.craftysoft.orderingsystem.orderprocessing.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import ru.craftysoft.orderingsystem.util.error.code.ExceptionCode;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class StackTraceHelper {

    public static <T> ListAppender<ILoggingEvent> listAppender(Class<T> clazz) {
        var listAppender = new ListAppender<ILoggingEvent>();
        listAppender.start();
        var logger = (Logger) LoggerFactory.getLogger(clazz);
        logger.addAppender(listAppender);
        return listAppender;
    }

    public static void thenErrorStacktrace(ListAppender<ILoggingEvent> listAppender,
                                           String fullExceptionCode,
                                           ExceptionCode<?> exceptionCode) {
        var hasValidationException = false;
        for (var event : listAppender.list) {
            var formattedMessage = event.getFormattedMessage();
            if (event.getLevel().equals(Level.ERROR)
                    && formattedMessage.contains(fullExceptionCode)
                    && formattedMessage.contains(exceptionCode.getMessage())) {
                hasValidationException = true;
            }
        }
        assertTrue(hasValidationException);
    }

    public static void thenErrorStacktrace(ListAppender<ILoggingEvent> listAppender,
                                           String fullExceptionCode,
                                           ExceptionCode<?> exceptionCode,
                                           String... messages) {
        var hasValidationException = false;
        for (var event : listAppender.list) {
            var formattedMessage = event.getFormattedMessage();
            var containsAllMessages = true;
            for (var message : messages) {
                if (!formattedMessage.contains(message)) {
                    containsAllMessages = false;
                    break;
                }
            }
            if (!containsAllMessages) {
                continue;
            }
            if (event.getLevel().equals(Level.ERROR)
                    && formattedMessage.contains(fullExceptionCode)
                    && formattedMessage.contains(exceptionCode.getMessage())) {
                hasValidationException = true;
            }
        }
        assertTrue(hasValidationException);
    }

}
