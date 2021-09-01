package ru.craftysoft.orderingsystem.util.error.logging;

import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import ru.craftysoft.orderingsystem.util.error.exception.BaseException;

import java.util.concurrent.CompletionException;

import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class ExceptionLoggerHelper {

    public static void logError(Logger logger, String point, Throwable throwable) {
        if (throwable instanceof BaseException baseException) {
            if (baseException.getOriginalCode() != null || baseException.getOriginalMessage() != null) {
                logger.error(
                        """
                                {}.thrown
                                code={},
                                message={},
                                originalCode={},
                                getOriginalMessage={}""",
                        point, baseException.getFullErrorCode(), baseException.getMessage(), baseException.getOriginalCode(), baseException.getOriginalMessage(), baseException);
            } else {
                logger.error(
                        """
                                {}.thrown
                                code={},
                                message={}""",
                        point, baseException.getFullErrorCode(), baseException.getMessage(), baseException);
            }
        } else if (throwable instanceof CompletionException completionException) {
            ofNullable(completionException.getCause())
                    .filter(cause -> cause instanceof BaseException)
                    .map(BaseException.class::cast)
                    .ifPresentOrElse(baseException -> {
                        if (baseException.getOriginalCode() != null || baseException.getOriginalMessage() != null) {
                            logger.error(
                                    """
                                            {}.thrown
                                            code={},
                                            message={},
                                            originalCode={},
                                            getOriginalMessage={}""",
                                    point, baseException.getFullErrorCode(), baseException.getMessage(), baseException.getOriginalCode(), baseException.getOriginalMessage(), throwable);
                        } else {
                            logger.error(
                                    """
                                            {}.thrown
                                            code={},
                                            message={}""",
                                    point, baseException.getFullErrorCode(), baseException.getMessage(), throwable);
                        }
                    }, () -> {
                        logger.error("{}.thrown", point, throwable);
                    });
        } else {
            logger.error("{}.thrown", point, throwable);
        }
    }

}
