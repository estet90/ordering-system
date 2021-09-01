package ru.craftysoft.orderingsystem.orderprocessing.logic.messagedriven;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import ru.craftysoft.orderingsystem.orderprocessing.proto.CompleteOrderRequest;
import ru.craftysoft.orderingsystem.orderprocessing.service.dao.OrderDaoAdapter;
import ru.craftysoft.orderingsystem.orderprocessing.service.redis.RedisClientAdapter;
import ru.craftysoft.orderingsystem.util.error.exception.RetryableException;
import ru.craftysoft.orderingsystem.util.uuid.UuidUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static ru.craftysoft.orderingsystem.orderprocessing.error.operation.ModuleOperationCode.COMPLETE_ORDER;
import static ru.craftysoft.orderingsystem.orderprocessing.service.redis.RedisClient.REDIS_MESSAGE_ID;
import static ru.craftysoft.orderingsystem.util.error.logging.ExceptionLoggerHelper.logError;
import static ru.craftysoft.orderingsystem.util.mdc.MdcKey.*;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

/**
 * заказ помечается как выполненный
 * при возникновении ошибки пытаемся выполнить повторный вызов. после истечения всех попыток переходим к {@link DecreaseExecutorAmountOperation}
 */
@Singleton
@Slf4j
public class CompleteOrderOperation {

    private final RedisClientAdapter redisClientAdapter;
    private final OrderDaoAdapter orderDaoAdapter;
    private final String processPoint = "CompleteOrderOperation.process";
    private final String processMessagePoint = "CompleteOrderOperation.processMessage";

    @Inject
    public CompleteOrderOperation(RedisClientAdapter redisClientAdapter, OrderDaoAdapter orderDaoAdapter) {
        this.redisClientAdapter = redisClientAdapter;
        this.orderDaoAdapter = orderDaoAdapter;
    }

    public CompletionStage<Void> process() {
        try (var ignored1 = MDC.putCloseable(TRACE_ID, UuidUtils.generateDefaultUuid());
             var ignored2 = MDC.putCloseable(SPAN_ID, UuidUtils.generateDefaultUuid());
             var ignored3 = MDC.putCloseable(OPERATION_NAME, COMPLETE_ORDER.name())) {
            return redisClientAdapter.listenCompleteOrderRequestMessages()
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

    private CompletableFuture<String> processMessage(Map.Entry<String, CompleteOrderRequest> entry) {
        try {
            MDC.put(REDIS_MESSAGE_ID, entry.getKey());
            return orderDaoAdapter.completeOrder(entry.getValue())
                    .thenApply(withMdc(v -> {
                        return "nop"; //тут может быть отправка какого-то уведомления на почту исполнителю и заказчику
                    }))
                    .exceptionallyCompose(withMdc(throwable -> {
                        logError(log, processMessagePoint, throwable);
                        if (throwable instanceof RetryableException retryableException) {
                            return retryWithRollback(entry, retryableException);
                        }
                        return rollback(entry);
                    }));
        } finally {
            MDC.remove(REDIS_MESSAGE_ID);
        }
    }

    private CompletionStage<String> retryWithRollback(Map.Entry<String, CompleteOrderRequest> entry, RetryableException e) {
        return redisClientAdapter.retryCompleteOrderRequestMessage(entry, e)
                .exceptionallyCompose((retryThrowable) -> {
                    logError(log, processMessagePoint + ".retry", retryThrowable);
                    return rollback(entry);
                });
    }

    private CompletionStage<String> rollback(Map.Entry<String, CompleteOrderRequest> entry) {
        return redisClientAdapter.sendMessageToDecreaseExecutorAmountStream(entry)
                .whenComplete(withMdc((unused, rollbackThrowable) -> {
                    if (rollbackThrowable != null) {
                        logError(log, processMessagePoint + ".rollback", rollbackThrowable);
                    } else {
                        log.info("{}.rollback.out", processMessagePoint);
                    }
                }));
    }
}
