package ru.craftysoft.orderingsystem.orderprocessing.logic.messagedriven;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import ru.craftysoft.orderingsystem.orderprocessing.proto.IncrementExecutorAmountRequest;
import ru.craftysoft.orderingsystem.orderprocessing.service.grpc.ExecutorServiceClientAdapter;
import ru.craftysoft.orderingsystem.orderprocessing.service.redis.RedisClientAdapter;
import ru.craftysoft.orderingsystem.util.error.exception.RetryableException;
import ru.craftysoft.orderingsystem.util.uuid.UuidUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

import static ru.craftysoft.orderingsystem.orderprocessing.error.operation.ModuleOperationCode.INCREMENT_EXECUTOR_AMOUNT;
import static ru.craftysoft.orderingsystem.orderprocessing.service.redis.RedisClient.REDIS_MESSAGE_ID;
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
                    .thenAccept(withMdc(requests -> {
                        if (!requests.isEmpty()) {
                            log.info("{}.in size={}", processPoint, requests.size());
                            requests.forEach(this::processMessage);
                            log.info("{}.out", processPoint);
                        }
                    }))
                    .whenComplete(withMdc((unused, throwable) -> {
                        if (throwable != null) {
                            log.error("{}.thrown", processPoint, throwable);
                        }
                    }));
        }
    }

    private void processMessage(Map.Entry<String, IncrementExecutorAmountRequest> entry) {
        try (var ignored = MDC.putCloseable(REDIS_MESSAGE_ID, entry.getKey())) {
            executorServiceClientAdapter.incrementAmount(entry.getValue())
                    .whenComplete((updateExecutorBalanceResponse, throwable) -> {
                        if (throwable != null) {
                            log.error("{}.thrown", processMessagePoint, throwable);
                            if (throwable instanceof RetryableException) {
                                retryWithRollback(entry, throwable);
                            } else {
                                rollback(entry);
                            }
                        } else {
                            redisClientAdapter.sendMessageToCompleteOrderStream(entry)
                                    .whenComplete((unused, nextStepThrowable) -> {
                                        if (nextStepThrowable != null) {
                                            log.error("{}.thrown", processMessagePoint, nextStepThrowable);
                                            retryWithRollback(entry, nextStepThrowable);
                                        }
                                    });
                        }
                    });
        }
    }

    private void retryWithRollback(Map.Entry<String, IncrementExecutorAmountRequest> entry, Throwable throwable) {
        redisClientAdapter.retryIncrementExecutorAmountRequestMessage(entry, throwable)
                .whenComplete((ignored, retryThrowable) -> {
                    if (retryThrowable != null) {
                        log.error("{}.retry.thrown", processMessagePoint, retryThrowable);
                        rollback(entry);
                    } else {
                        log.error("{}.retry.out", processMessagePoint);
                    }
                });
    }

    private void rollback(Map.Entry<String, IncrementExecutorAmountRequest> entry) {
        redisClientAdapter.sendMessageToIncrementCustomerAmountStream(entry)
                .whenComplete((unused, rollbackThrowable) -> {
                    if (rollbackThrowable != null) {
                        log.error("{}.rollback.thrown", processMessagePoint, rollbackThrowable);
                    } else {
                        log.info("{}.rollback.out", processMessagePoint);
                    }
                });
    }
}
