package ru.craftysoft.orderingsystem.orderprocessing.logic.messagedriven;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import ru.craftysoft.orderingsystem.orderprocessing.error.operation.ModuleOperationCode;
import ru.craftysoft.orderingsystem.orderprocessing.proto.DecreaseCustomerAmountRequest;
import ru.craftysoft.orderingsystem.orderprocessing.service.grpc.CustomerServiceClientAdapter;
import ru.craftysoft.orderingsystem.orderprocessing.service.redis.RedisClientAdapter;
import ru.craftysoft.orderingsystem.util.error.exception.RetryableException;
import ru.craftysoft.orderingsystem.util.uuid.UuidUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static ru.craftysoft.orderingsystem.orderprocessing.error.operation.ModuleOperationCode.DECREASE_CUSTOMER_AMOUNT;
import static ru.craftysoft.orderingsystem.orderprocessing.service.redis.RedisClient.REDIS_MESSAGE_ID;
import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.mapException;
import static ru.craftysoft.orderingsystem.util.error.logging.ExceptionLoggerHelper.logError;
import static ru.craftysoft.orderingsystem.util.mdc.MdcKey.*;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

/**
 * у заказчика уменьшается баланс.
 * в случае успеха переходим к {@link IncrementExecutorAmountOperation}.
 * при возникновении ошибки пытаемся выполнить повторный вызов. после истечения всех попыток переходим к {@link ReserveOrderOperation}.
 */
@Singleton
@Slf4j
public class DecreaseCustomerAmountOperation {

    private final RedisClientAdapter redisClientAdapter;
    private final CustomerServiceClientAdapter customerServiceClientAdapter;
    private final String processPoint = "DecreaseCustomerAmountOperation.process";
    private final String processMessagePoint = "DecreaseCustomerAmountOperation.processMessage";

    @Inject
    public DecreaseCustomerAmountOperation(RedisClientAdapter redisClientAdapter,
                                           CustomerServiceClientAdapter customerServiceClientAdapter) {
        this.redisClientAdapter = redisClientAdapter;
        this.customerServiceClientAdapter = customerServiceClientAdapter;
    }

    public CompletionStage<Void> process() {
        try (var ignored1 = MDC.putCloseable(TRACE_ID, UuidUtils.generateDefaultUuid());
             var ignored2 = MDC.putCloseable(SPAN_ID, UuidUtils.generateDefaultUuid());
             var ignored3 = MDC.putCloseable(OPERATION_NAME, DECREASE_CUSTOMER_AMOUNT.name())) {
            return redisClientAdapter.listenDecreaseCustomerAmountRequestMessages()
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

    private CompletableFuture<String> processMessage(Map.Entry<String, DecreaseCustomerAmountRequest> entry) {
        try (var ignored = MDC.putCloseable(REDIS_MESSAGE_ID, entry.getKey())) {
            var request = entry.getValue();
            return customerServiceClientAdapter.decreaseAmount(request)
                    .thenCompose(withMdc(updateCustomerBalanceResponse -> {
                        return redisClientAdapter.sendMessageToIncrementExecutorAmountStream(updateCustomerBalanceResponse, entry)
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

    private CompletionStage<String> retryWithRollback(Map.Entry<String, DecreaseCustomerAmountRequest> entry, Throwable throwable) {
        return redisClientAdapter.retryDecreaseCustomerAmountRequestMessage(entry, throwable)
                .exceptionallyCompose((retryThrowable) -> {
                    logError(log, processMessagePoint + ".retry", retryThrowable);
                    return rollback(entry);
                });
    }

    private CompletionStage<String> rollback(Map.Entry<String, DecreaseCustomerAmountRequest> entry) {
        return redisClientAdapter.sendMessageToReserveOrderStream(entry)
                .whenComplete(withMdc((unused, rollbackThrowable) -> {
                    if (rollbackThrowable != null) {
                        logError(log, processMessagePoint + ".rollback", rollbackThrowable);
                    }
                }));
    }

}
