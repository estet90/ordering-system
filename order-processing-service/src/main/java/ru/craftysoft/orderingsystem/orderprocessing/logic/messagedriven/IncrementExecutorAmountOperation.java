package ru.craftysoft.orderingsystem.orderprocessing.logic.messagedriven;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import ru.craftysoft.orderingsystem.orderprocessing.error.operation.ModuleOperationCode;
import ru.craftysoft.orderingsystem.orderprocessing.proto.IncrementExecutorAmountRequest;
import ru.craftysoft.orderingsystem.orderprocessing.service.grpc.ExecutorServiceClientAdapter;
import ru.craftysoft.orderingsystem.orderprocessing.service.redis.RedisClientAdapter;
import ru.craftysoft.orderingsystem.util.error.exception.RetryableException;
import ru.craftysoft.orderingsystem.util.uuid.UuidUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static ru.craftysoft.orderingsystem.orderprocessing.error.operation.ModuleOperationCode.INCREMENT_EXECUTOR_AMOUNT;
import static ru.craftysoft.orderingsystem.orderprocessing.service.redis.RedisClient.REDIS_MESSAGE_ID;
import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.mapException;
import static ru.craftysoft.orderingsystem.util.error.logging.ExceptionLoggerHelper.logError;
import static ru.craftysoft.orderingsystem.util.mdc.MdcKey.*;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

/**
 * увеличиваем баланс исполнителя.
 * в случае успеха переходим к {@link CompleteOrderOperation}.
 * при возникновении ошибки пытаемся выполнить повторный вызов. после истечения всех попыток переходим к {@link IncrementCustomerAmountOperation}
 */
@Singleton
@Slf4j
public class IncrementExecutorAmountOperation {

    private final RedisClientAdapter redisClientAdapter;
    private final ExecutorServiceClientAdapter executorServiceClientAdapter;
    private final String processPoint = "IncrementExecutorAmountOperation.process";
    private final String processMessagePoint = "IncrementExecutorAmountOperation.processMessage";

    @Inject
    public IncrementExecutorAmountOperation(RedisClientAdapter redisClientAdapter, ExecutorServiceClientAdapter executorServiceClientAdapter) {
        this.redisClientAdapter = redisClientAdapter;
        this.executorServiceClientAdapter = executorServiceClientAdapter;
    }

    public void process() {
        try (var ignored1 = MDC.putCloseable(TRACE_ID, UuidUtils.generateDefaultUuid());
             var ignored2 = MDC.putCloseable(SPAN_ID, UuidUtils.generateDefaultUuid());
             var ignored3 = MDC.putCloseable(OPERATION_NAME, INCREMENT_EXECUTOR_AMOUNT.name())) {
            redisClientAdapter.listenIncrementExecutorAmountRequestMessages()
                    .whenComplete(withMdc((unused, throwable) -> {
                        if (throwable != null) {
                            logError(log, processPoint, throwable);
                        }
                    }))
                    .thenCompose(withMdc(requests -> {
                        if (!requests.isEmpty()) {
                            log.info("{}.in size={}", processPoint, requests.size());
                            var futures = requests.stream()
                                    .map(this::processMessage)
                                    .toArray(value -> new CompletableFuture<?>[requests.size()]);
                            return CompletableFuture.allOf(futures)
                                    .whenComplete(withMdc((v, t) -> {
                                        log.info("{}.out", processPoint);
                                    }));
                        }
                        return new CompletableFuture<>();
                    }));
        }
    }

    private CompletableFuture<String> processMessage(Map.Entry<String, IncrementExecutorAmountRequest> entry) {
        try (var ignored = MDC.putCloseable(REDIS_MESSAGE_ID, entry.getKey())) {
            return executorServiceClientAdapter.incrementAmount(entry.getValue())
                    .thenCompose(withMdc(updateExecutorBalanceResponse -> {
                        return redisClientAdapter.sendMessageToCompleteOrderStream(entry)
                                .exceptionallyCompose(withMdc(nextStepThrowable -> {
                                    logError(log, processMessagePoint, nextStepThrowable);
                                    if (mapException(nextStepThrowable, ModuleOperationCode::resolve) instanceof RetryableException) {
                                        return retryWithRollback(entry, nextStepThrowable);
                                    }
                                    return rollback(entry);
                                }));
                    }))
                    .exceptionallyCompose(throwable -> {
                        logError(log, processMessagePoint, throwable);
                        if (mapException(throwable, ModuleOperationCode::resolve) instanceof RetryableException) {
                            return retryWithRollback(entry, throwable);
                        }
                        return rollback(entry);
                    });
        }
    }

    private CompletionStage<String> retryWithRollback(Map.Entry<String, IncrementExecutorAmountRequest> entry, Throwable throwable) {
        return redisClientAdapter.retryIncrementExecutorAmountRequestMessage(entry, throwable)
                .exceptionallyCompose((retryThrowable) -> {
                    logError(log, processMessagePoint + ".retry", retryThrowable);
                    return rollback(entry);
                });
    }

    private CompletionStage<String> rollback(Map.Entry<String, IncrementExecutorAmountRequest> entry) {
        return redisClientAdapter.sendMessageToIncrementCustomerAmountStream(entry)
                .whenComplete(withMdc((unused, rollbackThrowable) -> {
                    if (rollbackThrowable != null) {
                        logError(log, processMessagePoint + ".rollback", rollbackThrowable);
                    } else {
                        log.info("{}.rollback.out", processMessagePoint);
                    }
                }));
    }
}
