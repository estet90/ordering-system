package ru.craftysoft.orderingsystem.orderprocessing.logic.messagedriven;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import ru.craftysoft.orderingsystem.orderprocessing.proto.DecreaseCustomerAmountRequest;
import ru.craftysoft.orderingsystem.orderprocessing.service.grpc.CustomerServiceClientAdapter;
import ru.craftysoft.orderingsystem.orderprocessing.service.redis.RedisClientAdapter;
import ru.craftysoft.orderingsystem.util.error.exception.RetryableException;
import ru.craftysoft.orderingsystem.util.uuid.UuidUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

import static ru.craftysoft.orderingsystem.orderprocessing.error.operation.ModuleOperationCode.DECREASE_CUSTOMER_AMOUNT;
import static ru.craftysoft.orderingsystem.orderprocessing.service.redis.RedisClient.REDIS_MESSAGE_ID;
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

    public void process() {
        try (var ignored1 = MDC.putCloseable(TRACE_ID, UuidUtils.generateDefaultUuid());
             var ignored2 = MDC.putCloseable(SPAN_ID, UuidUtils.generateDefaultUuid());
             var ignored3 = MDC.putCloseable(OPERATION_NAME, DECREASE_CUSTOMER_AMOUNT.name())) {
            redisClientAdapter.listenDecreaseCustomerAmountRequestMessages()
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

    private void processMessage(Map.Entry<String, DecreaseCustomerAmountRequest> entry) {
        try (var ignored = MDC.putCloseable(REDIS_MESSAGE_ID, entry.getKey())) {
            var request = entry.getValue();
            customerServiceClientAdapter.decreaseAmount(request)
                    .whenComplete(withMdc((updateCustomerBalanceResponse, throwable) -> {
                        if (throwable != null) {
                            log.error("{}.thrown", processMessagePoint, throwable);
                            if (throwable instanceof RetryableException) {
                                retryWithRollback(entry, throwable);
                            } else {
                                rollback(entry);
                            }
                        } else {
                            redisClientAdapter.sendMessageToIncrementExecutorAmountStream(updateCustomerBalanceResponse, entry)
                                    .whenComplete(withMdc((unused, nextStepThrowable) -> {
                                        if (nextStepThrowable != null) {
                                            log.error("{}.thrown", processMessagePoint, nextStepThrowable);
                                            if (nextStepThrowable instanceof RetryableException) {
                                                retryWithRollback(entry, nextStepThrowable);
                                            } else {
                                                rollback(entry);
                                            }
                                        }
                                    }));
                        }
                    }));
        }
    }

    private void retryWithRollback(Map.Entry<String, DecreaseCustomerAmountRequest> entry, Throwable throwable) {
        redisClientAdapter.retryDecreaseCustomerAmountRequestMessage(entry, throwable)
                .whenComplete(withMdc((ignored, retryThrowable) -> {
                    if (retryThrowable != null) {
                        log.error("{}.retry.thrown", processMessagePoint, retryThrowable);
                        rollback(entry);
                    } else {
                        log.error("{}.retry.out", processMessagePoint);
                    }
                }));
    }

    private void rollback(Map.Entry<String, DecreaseCustomerAmountRequest> entry) {
        redisClientAdapter.sendMessageToReserveOrderStream(entry)
                .whenComplete(withMdc((unused, rollbackThrowable) -> {
                    if (rollbackThrowable != null) {
                        log.error("{}.rollback.thrown", processMessagePoint, rollbackThrowable);
                    }
                }));
    }

}
