package ru.craftysoft.orderingsystem.orderprocessing.logic.messagedriven;

import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.orderprocessing.proto.IncrementExecutorAmountRequest;
import ru.craftysoft.orderingsystem.orderprocessing.service.grpc.ExecutorServiceClientAdapter;
import ru.craftysoft.orderingsystem.orderprocessing.service.redis.RedisClientAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;

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

    @Inject
    public IncrementExecutorAmountOperation(RedisClientAdapter redisClientAdapter, ExecutorServiceClientAdapter executorServiceClientAdapter) {
        this.redisClientAdapter = redisClientAdapter;
        this.executorServiceClientAdapter = executorServiceClientAdapter;
    }

    public void process() {
        redisClientAdapter.listenIncrementExecutorAmountRequestMessages()
                .thenAccept(requests -> {
                    if (!requests.isEmpty()) {
                        log.info("IncrementExecutorAmountOperation.process.in size={}", requests.size());
                        requests.forEach(this::processMessage);
                        log.info("IncrementExecutorAmountOperation.process.out");
                    }
                })
                .whenComplete((unused, throwable) -> {
                    if (throwable != null) {
                        log.error("IncrementExecutorAmountOperation.process.thrown", throwable);
                    }
                });
    }

    private void processMessage(IncrementExecutorAmountRequest request) {
        executorServiceClientAdapter.incrementAmount(request)
                .whenComplete((updateExecutorBalanceResponse, throwable) -> {
                    if (throwable != null) {
                        log.error("IncrementExecutorAmountOperation.processMessage.thrown", throwable);
                        retryWithRollback(request, throwable);
                    } else {
                        redisClientAdapter.sendMessageToCompleteOrderStream(request)
                                .whenComplete((unused, nextStepThrowable) -> {
                                    if (nextStepThrowable != null) {
                                        log.error("IncrementExecutorAmountOperation.processMessage.thrown", nextStepThrowable);
                                        retryWithRollback(request, nextStepThrowable);
                                    }
                                });
                    }
                });
    }

    private void retryWithRollback(IncrementExecutorAmountRequest request, Throwable throwable) {
        redisClientAdapter.retryIncrementExecutorAmountRequestMessage(request, throwable)
                .whenComplete((ignored, retryThrowable) -> {
                    if (retryThrowable != null) {
                        log.error("IncrementExecutorAmountOperation.processMessage.retry.thrown", retryThrowable);
                        redisClientAdapter.sendMessageToIncrementCustomerAmountStream(request)
                                .whenComplete((unused, rollbackThrowable) -> {
                                    if (rollbackThrowable != null) {
                                        log.error("IncrementExecutorAmountOperation.processMessage.rollback.thrown", rollbackThrowable);
                                    } else {
                                        log.info("IncrementExecutorAmountOperation.processMessage.rollback.out");
                                    }
                                });
                    } else {
                        log.error("IncrementExecutorAmountOperation.processMessage.retry.out");
                    }
                });
    }
}
