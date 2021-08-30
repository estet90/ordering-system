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

import static ru.craftysoft.orderingsystem.orderprocessing.error.operation.ModuleOperationCode.COMPLETE_ORDER;
import static ru.craftysoft.orderingsystem.orderprocessing.service.redis.RedisClient.REDIS_MESSAGE_ID;
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

    public void process() {
        try (var ignored1 = MDC.putCloseable(TRACE_ID, UuidUtils.generateDefaultUuid());
             var ignored2 = MDC.putCloseable(SPAN_ID, UuidUtils.generateDefaultUuid());
             var ignored3 = MDC.putCloseable(OPERATION_NAME, COMPLETE_ORDER.name())) {
            redisClientAdapter.listenCompleteOrderRequestMessages()
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

    private void processMessage(Map.Entry<String, CompleteOrderRequest> entry) {
        try {
            MDC.put(REDIS_MESSAGE_ID, entry.getKey());
            orderDaoAdapter.completeOrder(entry.getValue());
        } catch (RetryableException e) {
            log.error("{}.thrown", processMessagePoint, e);
            retryWithRollback(entry, e);
        } catch (Exception e) {
            log.error("{}.thrown", processMessagePoint, e);
            rollback(entry);
        } finally {
            MDC.remove(REDIS_MESSAGE_ID);
        }
    }

    private void retryWithRollback(Map.Entry<String, CompleteOrderRequest> entry, RetryableException e) {
        redisClientAdapter.retryCompleteOrderRequestMessage(entry, e)
                .whenComplete(withMdc((ignored, retryThrowable) -> {
                    if (retryThrowable != null) {
                        log.error("{}.retry.thrown", processMessagePoint, retryThrowable);
                        rollback(entry);
                    } else {
                        log.error("{}.retry.out", processMessagePoint);
                    }
                }));
    }

    private void rollback(Map.Entry<String, CompleteOrderRequest> entry) {
        redisClientAdapter.sendMessageToDecreaseExecutorAmountStream(entry)
                .whenComplete(withMdc((unused, throwable) -> {
                    if (throwable != null) {
                        log.error("{}.rollback.thrown", processMessagePoint, throwable);
                    }
                }));
    }
}
