package ru.craftysoft.orderingsystem.orderprocessing.logic.messagedriven;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import ru.craftysoft.orderingsystem.orderprocessing.proto.ReserveOrderRequest;
import ru.craftysoft.orderingsystem.orderprocessing.service.dao.OrderDaoAdapter;
import ru.craftysoft.orderingsystem.orderprocessing.service.redis.RedisClientAdapter;
import ru.craftysoft.orderingsystem.util.uuid.UuidUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

import static ru.craftysoft.orderingsystem.orderprocessing.error.operation.ModuleOperationCode.RESERVE_ORDER;
import static ru.craftysoft.orderingsystem.orderprocessing.service.redis.RedisClient.REDIS_MESSAGE_ID;
import static ru.craftysoft.orderingsystem.util.error.logging.ExceptionLoggerHelper.logError;
import static ru.craftysoft.orderingsystem.util.mdc.MdcKey.*;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

/**
 * заказ снова резервируется. последняя операция при откате
 */
@Singleton
@Slf4j
public class ReserveOrderOperation {

    private final RedisClientAdapter redisClientAdapter;
    private final OrderDaoAdapter orderDaoAdapter;
    private final String processPoint = "ReserveOrderOperation.process";
    private final String processMessagePoint = "ReserveOrderOperation.processMessage";

    @Inject
    public ReserveOrderOperation(RedisClientAdapter redisClientAdapter, OrderDaoAdapter orderDaoAdapter) {
        this.redisClientAdapter = redisClientAdapter;
        this.orderDaoAdapter = orderDaoAdapter;
    }

    public void process() {
        try (var ignored1 = MDC.putCloseable(TRACE_ID, UuidUtils.generateDefaultUuid());
             var ignored2 = MDC.putCloseable(SPAN_ID, UuidUtils.generateDefaultUuid());
             var ignored3 = MDC.putCloseable(OPERATION_NAME, RESERVE_ORDER.name())) {
            redisClientAdapter.listenReserveOrderRequestMessages()
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

    private void processMessage(Map.Entry<String, ReserveOrderRequest> entry) {
        try {
            MDC.put(REDIS_MESSAGE_ID, entry.getKey());
            orderDaoAdapter.reserveOrder(entry.getValue());
        } catch (Exception e) {
            logError(log, processPoint, e);
            redisClientAdapter.retryReserveOrderRequestMessage(entry, e)
                    .whenComplete(withMdc((ignored, retryThrowable) -> {
                        if (retryThrowable != null) {
                            logError(log, processMessagePoint + ".retry", retryThrowable);
                        } else {
                            log.info("{}.retry.out", processMessagePoint);
                        }
                    }));
        } finally {
            MDC.remove(REDIS_MESSAGE_ID);
        }
    }

}
