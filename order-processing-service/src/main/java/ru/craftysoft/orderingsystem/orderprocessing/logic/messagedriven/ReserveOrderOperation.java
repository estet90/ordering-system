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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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

    public CompletionStage<Void> process() {
        try (var ignored1 = MDC.putCloseable(TRACE_ID, UuidUtils.generateDefaultUuid());
             var ignored2 = MDC.putCloseable(SPAN_ID, UuidUtils.generateDefaultUuid());
             var ignored3 = MDC.putCloseable(OPERATION_NAME, RESERVE_ORDER.name())) {
            return redisClientAdapter.listenReserveOrderRequestMessages()
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

    private CompletableFuture<String> processMessage(Map.Entry<String, ReserveOrderRequest> entry) {
        try {
            MDC.put(REDIS_MESSAGE_ID, entry.getKey());
            return orderDaoAdapter.reserveOrder(entry.getValue())
                    .thenApply(v -> "nop")
                    .exceptionallyCompose(withMdc(throwable -> {
                        return redisClientAdapter.retryReserveOrderRequestMessage(entry, throwable)
                                .whenComplete(withMdc((ignored, retryThrowable) -> {
                                    if (retryThrowable != null) {
                                        logError(log, processMessagePoint + ".retry", retryThrowable);
                                    } else {
                                        log.info("{}.retry.out", processMessagePoint);
                                    }
                                }));
                    }));
        } finally {
            MDC.remove(REDIS_MESSAGE_ID);
        }
    }

}
