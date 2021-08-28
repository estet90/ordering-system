package ru.craftysoft.orderingsystem.gateway.module;

import dagger.Module;
import dagger.Provides;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import ru.craftysoft.orderingsystem.customer.proto.CustomerServiceGrpc;
import ru.craftysoft.orderingsystem.executor.proto.ExecutorServiceGrpc;
import ru.craftysoft.orderingsystem.order.proto.OrderServiceGrpc;
import ru.craftysoft.orderingsystem.user.proto.UserServiceGrpc;
import ru.craftysoft.orderingsystem.util.properties.PropertyResolver;

import javax.inject.Named;
import javax.inject.Singleton;

@Module
public class GrpcClientModule {

    @Provides
    @Singleton
    @Named("userServiceManagedChannel")
    static ManagedChannel userServiceManagedChannel(PropertyResolver propertyResolver) {
        return ManagedChannelBuilder
                .forAddress(
                        propertyResolver.getStringProperty("grpc.user-service.host"),
                        propertyResolver.getIntProperty("grpc.user-service.port")
                )
                .usePlaintext()
                .build();
    }

    @Provides
    @Singleton
    static UserServiceGrpc.UserServiceStub userServiceStub(@Named("userServiceManagedChannel") ManagedChannel managedChannel) {
        return UserServiceGrpc.newStub(managedChannel);
    }

    @Provides
    @Singleton
    @Named("orderServiceManagedChannel")
    static ManagedChannel orderServiceManagedChannel(PropertyResolver propertyResolver) {
        return ManagedChannelBuilder
                .forAddress(
                        propertyResolver.getStringProperty("grpc.order-service.host"),
                        propertyResolver.getIntProperty("grpc.order-service.port")
                )
                .usePlaintext()
                .build();
    }

    @Provides
    @Singleton
    static OrderServiceGrpc.OrderServiceStub orderServiceStub(@Named("orderServiceManagedChannel") ManagedChannel managedChannel) {
        return OrderServiceGrpc.newStub(managedChannel);
    }

    @Provides
    @Singleton
    @Named("customerServiceManagedChannel")
    static ManagedChannel customerServiceManagedChannel(PropertyResolver propertyResolver) {
        return ManagedChannelBuilder
                .forAddress(
                        propertyResolver.getStringProperty("grpc.customer-service.host"),
                        propertyResolver.getIntProperty("grpc.customer-service.port")
                )
                .usePlaintext()
                .build();
    }

    @Provides
    @Singleton
    static CustomerServiceGrpc.CustomerServiceStub customerServiceStub(@Named("customerServiceManagedChannel") ManagedChannel managedChannel) {
        return CustomerServiceGrpc.newStub(managedChannel);
    }

    @Provides
    @Singleton
    @Named("executorServiceManagedChannel")
    static ManagedChannel executorServiceManagedChannel(PropertyResolver propertyResolver) {
        return ManagedChannelBuilder
                .forAddress(
                        propertyResolver.getStringProperty("grpc.executor-service.host"),
                        propertyResolver.getIntProperty("grpc.executor-service.port")
                )
                .usePlaintext()
                .build();
    }

    @Provides
    @Singleton
    static ExecutorServiceGrpc.ExecutorServiceStub executorServiceStub(@Named("executorServiceManagedChannel") ManagedChannel managedChannel) {
        return ExecutorServiceGrpc.newStub(managedChannel);
    }

}
