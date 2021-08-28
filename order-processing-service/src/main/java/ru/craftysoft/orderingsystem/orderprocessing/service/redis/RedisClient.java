package ru.craftysoft.orderingsystem.orderprocessing.service.redis;

import io.lettuce.core.Consumer;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

@Singleton
@Slf4j
public class RedisClient {

    private final Consumer<String> applicationConsumer;

    @Inject
    public RedisClient(Consumer<String> applicationConsumer) {
        this.applicationConsumer = applicationConsumer;
    }

    public <T> CompletionStage<String> sendMessage(StatefulRedisConnection<String, byte[]> connection,
                                                   String streamKey,
                                                   T message,
                                                   Function<T, byte[]> mapper,
                                                   Function<T, String> logMapper) {
        if (log.isDebugEnabled()) {
            var loggedMessage = logMapper.apply(message);
            log.debug("RedisClient.sendMessage.in stream={} message={}", streamKey, loggedMessage);
        }
        return connection.async()
                .xadd(streamKey, "key", mapper.apply(message))
                .whenComplete((s, throwable) -> {
                    if (throwable != null) {
                        log.error("RedisClient.sendMessage.thrown {}", throwable.getMessage());
                        throw new RuntimeException(throwable);
                    }
                    log.debug("RedisClient.sendMessage.out");
                });
    }

    public <T> CompletionStage<List<T>> subscribe(StatefulRedisConnection<String, byte[]> connection,
                                                  String streamKey,
                                                  Function<byte[], T> mapper,
                                                  Function<T, String> logMapper) {
        var asyncCommands = connection.async();
        return asyncCommands
                .xreadgroup(applicationConsumer, XReadArgs.StreamOffset.lastConsumed(streamKey))
                .handleAsync((messages, throwable) -> {
                    if (messages != null && !messages.isEmpty()) {
                        log.debug("RedisClient.subscribe.in");
                        var result = messages.stream()
                                .map(message -> {
                                    asyncCommands.xack(streamKey, applicationConsumer.getGroup(), message.getId());
                                    try {
                                        var resultMessage = mapper.apply(message.getBody().values().iterator().next());
                                        if (log.isDebugEnabled()) {
                                            var loggedMessage = logMapper.apply(resultMessage);
                                            log.debug("RedisClient.subscribeMessage message={}", loggedMessage);
                                        }
                                        return resultMessage;
                                    } catch (Exception e) {
                                        log.error("RedisClient.subscribeMessage.thrown", e);
                                        return null;
                                    }
                                })
                                .filter(Objects::nonNull)
                                .toList();
                        log.debug("RedisClient.subscribe.out size={}", result.size());
                        return result;
                    }
                    return List.of();
                });
    }
}
