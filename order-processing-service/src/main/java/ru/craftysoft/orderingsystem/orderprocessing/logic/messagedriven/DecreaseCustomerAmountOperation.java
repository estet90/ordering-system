package ru.craftysoft.orderingsystem.orderprocessing.logic.messagedriven;

import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.orderprocessing.proto.DecreaseCustomerAmountRequest;
import ru.craftysoft.orderingsystem.orderprocessing.service.grpc.CustomerServiceClientAdapter;
import ru.craftysoft.orderingsystem.orderprocessing.service.redis.RedisClientAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;

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

    @Inject
    public DecreaseCustomerAmountOperation(RedisClientAdapter redisClientAdapter,
                                           CustomerServiceClientAdapter customerServiceClientAdapter) {
        this.redisClientAdapter = redisClientAdapter;
        this.customerServiceClientAdapter = customerServiceClientAdapter;
    }

    public void process() {
        redisClientAdapter.listenDecreaseCustomerAmountRequestMessages()
                .thenAccept(requests -> {
                    if (!requests.isEmpty()) {
                        log.info("DecreaseCustomerAmountOperation.process.in size={}", requests.size());
                        requests.forEach(this::processMessage);
                        log.info("DecreaseCustomerAmountOperation.process.out");
                    }
                })
                .whenComplete((unused, throwable) -> {
                    if (throwable != null) {
                        log.error("DecreaseCustomerAmountOperation.process.thrown", throwable);
                    }
                });
    }

    private void processMessage(DecreaseCustomerAmountRequest request) {
        customerServiceClientAdapter.decreaseAmount(request)
                .whenComplete((updateCustomerBalanceResponse, throwable) -> {
                    if (throwable != null) {
                        log.error("DecreaseCustomerAmountOperation.processMessage.thrown", throwable);
                        retryWithRollback(request, throwable);
                    } else {
                        redisClientAdapter.sendMessageToIncrementExecutorAmountStream(updateCustomerBalanceResponse, request)
                                .whenComplete((unused, nextStepThrowable) -> {
                                    if (nextStepThrowable != null) {
                                        log.error("DecreaseCustomerAmountOperation.processMessage.thrown", nextStepThrowable);
                                        retryWithRollback(request, nextStepThrowable);
                                    }
                                });
                    }
                });
    }

    private void retryWithRollback(DecreaseCustomerAmountRequest request, Throwable throwable) {
        redisClientAdapter.retryDecreaseCustomerAmountRequestMessage(request, throwable)
                .whenComplete((ignored, retryThrowable) -> {
                    if (retryThrowable != null) {
                        log.error("DecreaseCustomerAmountOperation.processMessage.retry.thrown", retryThrowable);
                        redisClientAdapter.sendMessageToReserveOrderStream(request)
                                .whenComplete((unused1, rollbackThrowable) -> {
                                    if (rollbackThrowable != null) {
                                        log.error("DecreaseCustomerAmountOperation.processMessage.rollback.thrown", rollbackThrowable);
                                    }
                                });
                    } else {
                        log.error("DecreaseCustomerAmountOperation.processMessage.retry.out");
                    }
                });
    }

}
