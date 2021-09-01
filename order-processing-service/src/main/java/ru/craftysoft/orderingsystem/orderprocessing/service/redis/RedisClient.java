package ru.craftysoft.orderingsystem.orderprocessing.service.redis;

import io.lettuce.core.Consumer;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.support.BoundedAsyncPool;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static ru.craftysoft.orderingsystem.orderprocessing.error.exception.InvocationExceptionCode.REDIS;
import static ru.craftysoft.orderingsystem.orderprocessing.error.operation.ModuleOperationCode.resolve;
import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.newRetryableException;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

@Singleton
@Slf4j
public class RedisClient {

    private final Consumer<String> applicationConsumer;
    private final BoundedAsyncPool<StatefulRedisConnection<String, byte[]>> redisPool;

    public static final String REDIS_MESSAGE_ID = "redisMessageId";

    @Inject
    public RedisClient(Consumer<String> applicationConsumer, BoundedAsyncPool<StatefulRedisConnection<String, byte[]>> redisPool) {
        this.applicationConsumer = applicationConsumer;
        this.redisPool = redisPool;
    }

    public <T> CompletionStage<String> sendMessage(String streamKey,
                                                   String messageId,
                                                   T message,
                                                   Function<T, byte[]> mapper,
                                                   Function<T, String> logMapper) {
        try (var ignored = MDC.putCloseable(REDIS_MESSAGE_ID, messageId)) {
            log.debug("RedisClient.sendMessage.in");
            return redisPool.acquire()
                    .thenCompose(connection -> connection.async()
                            .xadd(
                                    streamKey,
                                    "payload", mapper.apply(message),
                                    REDIS_MESSAGE_ID, messageId.getBytes(StandardCharsets.UTF_8)
                            )
                            .whenComplete(withMdc((s, throwable) -> {
                                if (throwable != null) {
                                    log.error("RedisClient.sendMessage.thrown {}", throwable.getMessage());
                                    throw newRetryableException(throwable, resolve(), REDIS, throwable.getMessage());
                                }
                                if (log.isDebugEnabled()) {
                                    var loggedMessage = logMapper.apply(message);
                                    log.debug("RedisClient.sendMessage.out stream={} message={}", streamKey, loggedMessage);
                                }
                            }))
                            .thenApply(withMdc(msgId -> {
                                redisPool.release(connection);
                                return msgId;
                            }))
                    );
        }
    }

    public <T> CompletionStage<List<Map.Entry<String, T>>> subscribe(String streamKey,
                                                                     Function<byte[], T> mapper,
                                                                     Function<T, String> logMapper) {
        return redisPool.acquire()
                .thenCompose(connection -> {
                    var asyncCommands = connection.async();
                    return asyncCommands
                            .xreadgroup(applicationConsumer, XReadArgs.StreamOffset.lastConsumed(streamKey))
                            .handleAsync(withMdc((messages, throwable) -> {
                                if (messages != null && !messages.isEmpty()) {
                                    log.debug("RedisClient.subscribe.in");
                                    var result = messages.stream()
                                            .map(message -> {
                                                asyncCommands.xack(streamKey, applicationConsumer.getGroup(), message.getId());
                                                try {
                                                    var resultMessage = mapper.apply(message.getBody().get("payload"));
                                                    var messageId = new String(message.getBody().get(REDIS_MESSAGE_ID), StandardCharsets.UTF_8);
                                                    MDC.put(REDIS_MESSAGE_ID, messageId);
                                                    if (log.isDebugEnabled()) {
                                                        var loggedMessage = logMapper.apply(resultMessage);
                                                        log.debug("RedisClient.subscribeMessage stream={} message={}", streamKey, loggedMessage);
                                                    }
                                                    return Map.entry(messageId, resultMessage);
                                                } catch (Exception e) {
                                                    log.error("RedisClient.subscribeMessage.thrown", e);
                                                    return null;
                                                } finally {
                                                    MDC.remove(REDIS_MESSAGE_ID);
                                                }
                                            })
                                            .filter(Objects::nonNull)
                                            .toList();
                                    log.debug("RedisClient.subscribe.out size={}", result.size());
                                    return result;
                                }
                                return List.<Map.Entry<String, T>>of();
                            }))
                            .thenApply(withMdc((list) -> {
                                redisPool.release(connection);
                                return list;
                            }));
                });
    }
}
