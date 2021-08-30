package ru.craftysoft.orderingsystem.orderprocessing.module;

import dagger.Module;
import dagger.Provides;
import io.lettuce.core.Consumer;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.support.AsyncObjectFactory;
import io.lettuce.core.support.BoundedAsyncPool;
import io.lettuce.core.support.BoundedPoolConfig;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import ru.craftysoft.orderingsystem.orderprocessing.testcontainer.RedisContainer;
import ru.craftysoft.orderingsystem.util.properties.PropertyResolver;

import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Module
public class TestRedisModule {

    @Provides
    @Singleton
    static Supplier<StatefulRedisConnection<String, String>> redisConnectionFactory(RedisClient redisClient) {
        var redisUrl = RedisContainer.INSTANCE.getHosts();
        return () -> redisClient.connect(RedisURI.create(redisUrl));
    }

    @Provides
    @Singleton
    static RedisClient redisClient() {
        return RedisClient.create();
    }

    @Provides
    @Singleton
    static BoundedAsyncPool<StatefulRedisConnection<String, byte[]>> redisPool(RedisClient redisClient,
                                                                               RedisCodec<String, byte[]> stringBytesRedisCodec) {
        var redisUrl = RedisContainer.INSTANCE.getHosts();
        var connectionSupplier = new AsyncObjectFactory<StatefulRedisConnection<String, byte[]>>() {
            @Override
            public CompletableFuture<StatefulRedisConnection<String, byte[]>> create() {
                return redisClient.connectAsync(stringBytesRedisCodec, RedisURI.create(redisUrl)).toCompletableFuture();
            }

            @Override
            public CompletableFuture<Void> destroy(StatefulRedisConnection<String, byte[]> object) {
                return object.closeAsync();
            }

            @Override
            public CompletableFuture<Boolean> validate(StatefulRedisConnection<String, byte[]> object) {
                return CompletableFuture.completedFuture(object.isOpen());
            }
        };
        return new BoundedAsyncPool<>(connectionSupplier, BoundedPoolConfig.create());
    }

    @Provides
    @Singleton
    static RedisCodec<String, byte[]> stringBytesRedisCodec() {
        return new RedisCodec<>() {

            private static final byte[] EMPTY = new byte[0];

            @Override
            public String decodeKey(ByteBuffer bytes) {
                return Unpooled.wrappedBuffer(bytes).toString(StandardCharsets.UTF_8);
            }

            @Override
            public byte[] decodeValue(ByteBuffer buffer) {
                int remaining = buffer.remaining();
                if (remaining == 0) {
                    return EMPTY;
                }
                byte[] b = new byte[remaining];
                buffer.get(b);
                return b;
            }

            @Override
            public ByteBuffer encodeKey(String key) {
                if (key == null) {
                    return ByteBuffer.wrap(EMPTY);
                }
                var buffer = ByteBuffer.allocate(ByteBufUtil.utf8MaxBytes(key));
                var byteBuf = Unpooled.wrappedBuffer(buffer);
                byteBuf.clear();
                ByteBufUtil.writeUtf8(byteBuf, key);
                buffer.limit(byteBuf.writerIndex());
                return buffer;
            }

            @Override
            public ByteBuffer encodeValue(byte[] value) {
                return value == null
                        ? ByteBuffer.wrap(EMPTY)
                        : ByteBuffer.wrap(value);
            }
        };
    }

    @Provides
    @Singleton
    static Consumer<String> applicationConsumer(PropertyResolver propertyResolver) {
        return Consumer.from(propertyResolver.getStringProperty("redis.consumer.group.name"), "CONSUMER_NAME");
    }

}
