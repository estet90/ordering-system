package ru.craftysoft.orderingsystem.orderprocessing.logic.messagedriven;

import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.orderprocessing.proto.IncrementCustomerAmountRequest;
import ru.craftysoft.orderingsystem.orderprocessing.service.grpc.CustomerServiceClientAdapter;
import ru.craftysoft.orderingsystem.orderprocessing.service.redis.RedisClientAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * заказчику возвращаются деньги.
 * в случае успеха переходим к {@link ReserveOrderOperation}.
 * при возникновении ошибки пытаемся выполнить повторный вызов.
 */
@Singleton
@Slf4j
public class IncrementCustomerAmountOperation {

    private final RedisClientAdapter redisClientAdapter;
    private final CustomerServiceClientAdapter customerServiceClientAdapter;

    @Inject
    public IncrementCustomerAmountOperation(RedisClientAdapter redisClientAdapter, CustomerServiceClientAdapter customerServiceClientAdapter) {
        this.redisClientAdapter = redisClientAdapter;
        this.customerServiceClientAdapter = customerServiceClientAdapter;
    }

    public void process() {
        redisClientAdapter.listenIncrementCustomerAmountRequestMessages()
                .thenAccept(requests -> {
                    if (!requests.isEmpty()) {
                        log.info("IncrementCustomerAmountOperation.process.in size={}", requests.size());
                        requests.forEach(this::processMessage);
                        log.info("IncrementCustomerAmountOperation.process.out");
                    }
                })
                .whenComplete((unused, throwable) -> {
                    if (throwable != null) {
                        log.error("IncrementCustomerAmountOperation.process.thrown", throwable);
                    }
                });
    }

    private void processMessage(IncrementCustomerAmountRequest request) {
        customerServiceClientAdapter.incrementAmount(request)
                .whenComplete((updateCustomerBalanceResponse, throwable) -> {
                    if (throwable != null) {
                        log.error("IncrementCustomerAmountOperation.processMessage.thrown", throwable);
                        retry(request, throwable);
                    } else {
                        redisClientAdapter.sendMessageToReserveOrderStream(request)
                                .whenComplete((unused, nextStepThrowable) -> {
                                    if (nextStepThrowable != null) {
                                        log.error("IncrementCustomerAmountOperation.processMessage.thrown", nextStepThrowable);
                                        retry(request, nextStepThrowable);
                                    }
                                });
                    }
                });
    }

    private void retry(IncrementCustomerAmountRequest request, Throwable throwable) {
        redisClientAdapter.retryIncrementCustomerAmountRequestMessage(request, throwable)
                .whenComplete((ignored, retryThrowable) -> {
                    if (retryThrowable != null) {
                        log.error("IncrementCustomerAmountOperation.processMessage.retry.thrown", retryThrowable);
                    } else {
                        log.error("IncrementCustomerAmountOperation.processMessage.retry.out");
                    }
                });
    }
}
