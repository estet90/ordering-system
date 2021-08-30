package ru.craftysoft.orderingsystem.orderprocessing;

import io.lettuce.core.Consumer;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import ru.craftysoft.orderingsystem.orderprocessing.logic.RedisConsumerGroupInitOperation;
import ru.craftysoft.orderingsystem.orderprocessing.testcontainer.DbContainer;
import ru.craftysoft.orderingsystem.orderprocessing.util.TestDbHelper;
import ru.craftysoft.orderingsystem.util.db.DbHelper;
import ru.craftysoft.orderingsystem.util.properties.PropertyResolver;

import javax.inject.Inject;
import java.sql.Connection;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

public class OperationTest {

    @Inject
    protected PropertyResolver propertyResolver;
    @Inject
    protected RedisConsumerGroupInitOperation redisConsumerGroupInitOperation;
    @Inject
    protected DbHelper dbHelper;
    @Inject
    protected Supplier<StatefulRedisConnection<String, String>> redisConnectionFactory;
    @Inject
    protected Consumer<String> applicationConsumer;

    protected Supplier<Connection> connectionFactory() {
        return () -> TestDbHelper.getConnection(
                DbContainer.INSTANCE.getUrl(),
                DbContainer.INSTANCE.getUsername(),
                DbContainer.INSTANCE.getPassword()
        );
    }

    protected StreamMessage<String, String> thenSentMessage(String key) {
        try (var connection = redisConnectionFactory.get()) {
            var commands = connection.sync();
            var streamKey = propertyResolver.getStringProperty(key);
            var messages = commands
                    .xreadgroup(applicationConsumer, XReadArgs.StreamOffset.lastConsumed(streamKey));
            messages.forEach(m -> commands.xack(streamKey, applicationConsumer.getGroup(), m.getId()));
            assertThat(messages).hasSize(1);
            return messages.get(0);
        }
    }

}
