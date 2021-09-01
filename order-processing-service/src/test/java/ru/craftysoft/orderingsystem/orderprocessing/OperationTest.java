package ru.craftysoft.orderingsystem.orderprocessing;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.lettuce.core.Consumer;
import io.lettuce.core.api.StatefulRedisConnection;
import ru.craftysoft.orderingsystem.orderprocessing.logic.RedisConsumerGroupInitOperation;
import ru.craftysoft.orderingsystem.orderprocessing.service.redis.RedisClient;
import ru.craftysoft.orderingsystem.orderprocessing.testcontainer.DbContainer;
import ru.craftysoft.orderingsystem.orderprocessing.util.TestDbHelper;
import ru.craftysoft.orderingsystem.util.error.code.ExceptionCode;
import ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory;
import ru.craftysoft.orderingsystem.util.error.operation.OperationCode;
import ru.craftysoft.orderingsystem.util.error.type.ExceptionType;
import ru.craftysoft.orderingsystem.util.properties.PropertyResolver;

import javax.inject.Inject;
import java.sql.Connection;
import java.util.function.Supplier;

public class OperationTest {

    @Inject
    protected PropertyResolver propertyResolver;
    @Inject
    protected RedisConsumerGroupInitOperation redisConsumerGroupInitOperation;
    @Inject
    protected Supplier<StatefulRedisConnection<String, String>> redisConnectionFactory;
    @Inject
    protected Consumer<String> applicationConsumer;
    @Inject
    protected RedisClient redisClient;

    protected static final String SERVICE_CODE = "005";

    static {
        new ExceptionFactory(SERVICE_CODE);
    }

    protected Supplier<Connection> connectionFactory() {
        return () -> TestDbHelper.getConnection(
                DbContainer.INSTANCE.getUrl(),
                DbContainer.INSTANCE.getUsername(),
                DbContainer.INSTANCE.getPassword()
        );
    }

    protected String fullErrorCode(OperationCode operationCode, ExceptionType exceptionType, ExceptionCode<?> exceptionCode) {
        return String.join("-", SERVICE_CODE, operationCode.getCode(), exceptionType.getCode() + exceptionCode.getCode());
    }

    protected static Server grpcServer(int port, BindableService service) {
        return ServerBuilder.forPort(port)
                .addService(service)
                .build();
    }

}
