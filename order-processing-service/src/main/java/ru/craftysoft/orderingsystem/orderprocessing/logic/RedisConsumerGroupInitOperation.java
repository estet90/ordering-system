package ru.craftysoft.orderingsystem.orderprocessing.logic;

import io.lettuce.core.RedisBusyException;
import io.lettuce.core.RedisClient;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.util.properties.PropertyResolver;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Singleton
@Slf4j
public class RedisConsumerGroupInitOperation {

    private final Supplier<StatefulRedisConnection<String, String>> redisConnectionFactory;
    private final String consumerGroupName;
    private final List<String> streamsNames;

    @Inject
    public RedisConsumerGroupInitOperation(Supplier<StatefulRedisConnection<String, String>> redisConnectionFactory,
                                           PropertyResolver propertyResolver) {
        this.redisConnectionFactory = redisConnectionFactory;
        this.consumerGroupName = propertyResolver.getStringProperty("redis.consumer.group.name");
        streamsNames = Stream
                .of(
                        "redis.stream.increment-customer-amount.name",
                        "redis.stream.decrease-customer-amount.name",
                        "redis.stream.increment-executor-amount.name",
                        "redis.stream.decrease-executor-amount.name",
                        "redis.stream.reserve-order.name",
                        "redis.stream.complete-order.name"
                )
                .map(propertyResolver::getStringProperty)
                .toList();
    }

    public void process() {
        try (var connection = redisConnectionFactory.get()) {
            var commands = connection.sync();
            streamsNames.forEach(streamName -> {
                try {
                    var messageId = commands.xadd(streamName, "testKey", "testValue");
                    commands.xdel(streamName, messageId);
                    commands.xgroupCreate(XReadArgs.StreamOffset.from(streamName, "0-0"), consumerGroupName);
                } catch (RedisBusyException e) {
                    log.info(e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("???????????? ?????? ?????????????????????????? ??????????????????-????????????", e);
            throw new RuntimeException(e);
        }
    }
}
