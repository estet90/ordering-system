package ru.craftysoft.orderingsystem.orderprocessing.logic.messagedriven;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import ru.craftysoft.orderingsystem.orderprocessing.proto.IncrementCustomerAmountRequest;
import ru.craftysoft.orderingsystem.orderprocessing.service.grpc.CustomerServiceClientAdapter;
import ru.craftysoft.orderingsystem.orderprocessing.service.redis.RedisClientAdapter;
import ru.craftysoft.orderingsystem.util.uuid.UuidUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

import static ru.craftysoft.orderingsystem.orderprocessing.error.operation.ModuleOperationCode.INCREMENT_CUSTOMER_AMOUNT;
import static ru.craftysoft.orderingsystem.orderprocessing.service.redis.RedisClient.REDIS_MESSAGE_ID;
import static ru.craftysoft.orderingsystem.util.mdc.MdcKey.*;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

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
    private final String processPoint = "IncrementCustomerAmountOperation.process";
    private final String processMessagePoint = "IncrementCustomerAmountOperation.processMessage";

    @Inject
    public IncrementCustomerAmountOperation(RedisClientAdapter redisClientAdapter, CustomerServiceClientAdapter customerServiceClientAdapter) {
        this.redisClientAdapter = redisClientAdapter;
        this.customerServiceClientAdapter = customerServiceClientAdapter;
    }

    public void process() {
        try (var ignored1 = MDC.putCloseable(TRACE_ID, UuidUtils.generateDefaultUuid());
             var ignored2 = MDC.putCloseable(SPAN_ID, UuidUtils.generateDefaultUuid());
             var ignored3 = MDC.putCloseable(OPERATION_NAME, INCREMENT_CUSTOMER_AMOUNT.name())) {
            redisClientAdapter.listenIncrementCustomerAmountRequestMessages()
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

    private void processMessage(Map.Entry<String, IncrementCustomerAmountRequest> entry) {
        try (var ignored = MDC.putCloseable(REDIS_MESSAGE_ID, entry.getKey())) {
            customerServiceClientAdapter.incrementAmount(entry.getValue())
                    .whenComplete(withMdc((updateCustomerBalanceResponse, throwable) -> {
                        if (throwable != null) {
                            log.error("{}.thrown", processMessagePoint, throwable);
                            retry(entry, throwable);
                        } else {
                            redisClientAdapter.sendMessageToReserveOrderStreamInRollback(entry)
                                    .whenComplete(withMdc((unused, nextStepThrowable) -> {
                                        if (nextStepThrowable != null) {
                                            log.error("{}.thrown", processMessagePoint, nextStepThrowable);
                                            retry(entry, nextStepThrowable);
                                        }
                                    }));
                        }
                    }));
        }
    }

    private void retry(Map.Entry<String, IncrementCustomerAmountRequest> entry, Throwable throwable) {
        redisClientAdapter.retryIncrementCustomerAmountRequestMessage(entry, throwable)
                .whenComplete(withMdc((ignored, retryThrowable) -> {
                    if (retryThrowable != null) {
                        log.error("{}.retry.thrown", processMessagePoint, retryThrowable);
                    } else {
                        log.error("{}.retry.out", processMessagePoint);
                    }
                }));
    }
}
