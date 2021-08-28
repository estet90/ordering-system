package ru.craftysoft.orderingsystem.orderprocessing.logic.messagedriven;

import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.orderprocessing.proto.DecreaseExecutorAmountRequest;
import ru.craftysoft.orderingsystem.orderprocessing.service.grpc.ExecutorServiceClientAdapter;
import ru.craftysoft.orderingsystem.orderprocessing.service.redis.RedisClientAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;

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

    @Inject
    public DecreaseExecutorAmountOperation(RedisClientAdapter redisClientAdapter, ExecutorServiceClientAdapter executorServiceClientAdapter) {
        this.redisClientAdapter = redisClientAdapter;
        this.executorServiceClientAdapter = executorServiceClientAdapter;
    }

    public void process() {
        redisClientAdapter.listenDecreaseExecutorAmountRequestMessages()
                .thenAccept(requests -> {
                    if (!requests.isEmpty()) {
                        log.info("DecreaseExecutorAmountOperation.process.in size={}", requests.size());
                        requests.forEach(this::processMessage);
                        log.info("DecreaseExecutorAmountOperation.process.out");
                    }
                })
                .whenComplete((unused, throwable) -> {
                    if (throwable != null) {
                        log.error("DecreaseExecutorAmountOperation.process.thrown", throwable);
                    }
                });
    }

    private void processMessage(DecreaseExecutorAmountRequest request) {
        executorServiceClientAdapter.decreaseAmount(request)
                .whenComplete((updateCustomerBalanceResponse, throwable) -> {
                    if (throwable != null) {
                        log.error("DecreaseExecutorAmountOperation.processMessage.thrown", throwable);
                        retry(request, throwable);
                    } else {
                        redisClientAdapter.sendMessageToIncrementCustomerAmountStream(request)
                                .whenComplete((unused, nextStepThrowable) -> {
                                    if (nextStepThrowable != null) {
                                        log.error("DecreaseExecutorAmountOperation.processMessage.thrown", nextStepThrowable);
                                        retry(request, nextStepThrowable);
                                    }
                                });
                    }
                });
    }

    private void retry(DecreaseExecutorAmountRequest request, Throwable throwable) {
        redisClientAdapter.retryDecreaseExecutorAmountRequestMessage(request, throwable)
                .whenComplete((ignored, retryThrowable) -> {
                    if (retryThrowable != null) {
                        log.error("DecreaseExecutorAmountOperation.processMessage.retry.thrown", retryThrowable);
                    } else {
                        log.error("DecreaseExecutorAmountOperation.processMessage.retry.out");
                    }
                });
    }
}
