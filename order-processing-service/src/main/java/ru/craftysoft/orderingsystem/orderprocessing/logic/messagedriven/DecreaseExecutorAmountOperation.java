package ru.craftysoft.orderingsystem.orderprocessing.logic.messagedriven;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import ru.craftysoft.orderingsystem.orderprocessing.proto.DecreaseExecutorAmountRequest;
import ru.craftysoft.orderingsystem.orderprocessing.service.grpc.ExecutorServiceClientAdapter;
import ru.craftysoft.orderingsystem.orderprocessing.service.redis.RedisClientAdapter;
import ru.craftysoft.orderingsystem.util.uuid.UuidUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

import static ru.craftysoft.orderingsystem.orderprocessing.error.operation.ModuleOperationCode.DECREASE_EXECUTOR_AMOUNT;
import static ru.craftysoft.orderingsystem.orderprocessing.service.redis.RedisClient.REDIS_MESSAGE_ID;
import static ru.craftysoft.orderingsystem.util.error.logging.ExceptionLoggerHelper.logError;
import static ru.craftysoft.orderingsystem.util.mdc.MdcKey.*;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

/**
 * c исполнителя снимаются деньги.
 * в случае успеха переходим к {@link IncrementCustomerAmountOperation}.
 * при возникновении ошибки пытаемся выполнить повторный вызов.
 */
@Singleton
@Slf4j
public class DecreaseExecutorAmountOperation {

    private final RedisClientAdapter redisClientAdapter;
    private final ExecutorServiceClientAdapter executorServiceClientAdapter;
    private final String processPoint = "DecreaseExecutorAmountOperation.process";
    private final String processMessagePoint = "DecreaseExecutorAmountOperation.processMessage";

    @Inject
    public DecreaseExecutorAmountOperation(RedisClientAdapter redisClientAdapter, ExecutorServiceClientAdapter executorServiceClientAdapter) {
        this.redisClientAdapter = redisClientAdapter;
        this.executorServiceClientAdapter = executorServiceClientAdapter;
    }

    public void process() {
        try (var ignored1 = MDC.putCloseable(TRACE_ID, UuidUtils.generateDefaultUuid());
             var ignored2 = MDC.putCloseable(SPAN_ID, UuidUtils.generateDefaultUuid());
             var ignored3 = MDC.putCloseable(OPERATION_NAME, DECREASE_EXECUTOR_AMOUNT.name())) {
            redisClientAdapter.listenDecreaseExecutorAmountRequestMessages()
                    .thenAccept(withMdc(requests -> {
                        if (!requests.isEmpty()) {
                            log.info("{}.in size={}", processPoint, requests.size());
                            requests.forEach(this::processMessage);
                            log.info("{}.out", processPoint);
                        }
                    }))
                    .whenComplete(withMdc((unused, throwable) -> {
                        if (throwable != null) {
                            logError(log, processPoint, throwable);
                        }
                    }));
        }
    }

    private void processMessage(Map.Entry<String, DecreaseExecutorAmountRequest> entry) {
        try (var ignored = MDC.putCloseable(REDIS_MESSAGE_ID, entry.getKey())) {
            executorServiceClientAdapter.decreaseAmount(entry.getValue())
                    .whenComplete(withMdc((updateCustomerBalanceResponse, throwable) -> {
                        if (throwable != null) {
                            logError(log, processMessagePoint, throwable);
                            retry(entry, throwable);
                        } else {
                            redisClientAdapter.sendMessageToIncrementCustomerAmountStreamInRollback(entry)
                                    .whenComplete(withMdc((unused, nextStepThrowable) -> {
                                        if (nextStepThrowable != null) {
                                            logError(log, processMessagePoint, nextStepThrowable);
                                            retry(entry, nextStepThrowable);
                                        }
                                    }));
                        }
                    }));
        }
    }

    private void retry(Map.Entry<String, DecreaseExecutorAmountRequest> entry, Throwable throwable) {
        redisClientAdapter.retryDecreaseExecutorAmountRequestMessage(entry, throwable)
                .whenComplete(withMdc((ignored, retryThrowable) -> {
                    if (retryThrowable != null) {
                        logError(log, processMessagePoint + ".retry", retryThrowable);
                    } else {
                        log.info("{}.retry.out", processMessagePoint);
                    }
                }));
    }
}
