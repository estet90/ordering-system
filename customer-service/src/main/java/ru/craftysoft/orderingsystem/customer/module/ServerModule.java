package ru.craftysoft.orderingsystem.customer.module;

import dagger.Module;
import dagger.Provides;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.protobuf.services.ProtoReflectionService;
import ru.craftysoft.orderingsystem.customer.controller.CustomerController;
import ru.craftysoft.orderingsystem.util.grpc.LoggingServerInterceptor;
import ru.craftysoft.orderingsystem.util.properties.PropertyResolver;

import javax.inject.Singleton;

@Module
public class ServerModule {

    @Provides
    @Singleton
    static Server grpcServer(PropertyResolver propertyResolver, CustomerController controller) {
        var port = propertyResolver.getIntProperty("server.port");
        return ServerBuilder.forPort(port)
                .addService(ServerInterceptors.intercept(controller, new LoggingServerInterceptor()))
                .addService(ProtoReflectionService.newInstance())
                .build();
    }

}
