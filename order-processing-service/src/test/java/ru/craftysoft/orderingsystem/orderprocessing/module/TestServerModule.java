package ru.craftysoft.orderingsystem.orderprocessing.module;

import dagger.Module;
import dagger.Provides;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import ru.craftysoft.orderingsystem.customer.proto.CustomerServiceGrpc;
import ru.craftysoft.orderingsystem.customer.proto.UpdateCustomerBalanceRequest;
import ru.craftysoft.orderingsystem.customer.proto.UpdateCustomerBalanceResponse;
import ru.craftysoft.orderingsystem.customer.proto.UpdateCustomerBalanceResponseData;
import ru.craftysoft.orderingsystem.executor.proto.Error;
import ru.craftysoft.orderingsystem.executor.proto.*;
import ru.craftysoft.orderingsystem.util.grpc.ExceptionHelper;
import ru.craftysoft.orderingsystem.util.properties.PropertyResolver;

import javax.inject.Named;
import javax.inject.Singleton;

import static ru.craftysoft.orderingsystem.executor.proto.UpdateExecutorBalanceResponseData.Result.BALANCE_HAS_BEEN_CHANGED;
import static ru.craftysoft.orderingsystem.executor.proto.UpdateExecutorBalanceResponseData.Result.BALANCE_HAS_NOT_BEEN_CHANGED;

@Module
public class TestServerModule {

    @Provides
    @Singleton
    @Named("executorServiceSuccessServer")
    static Server executorServiceSuccessServer(PropertyResolver propertyResolver) {
        var controller = new ExecutorServiceGrpc.ExecutorServiceImplBase() {
            @Override
            public void updateExecutorBalance(UpdateExecutorBalanceRequest request, StreamObserver<UpdateExecutorBalanceResponse> responseObserver) {
                var balance = request.hasDecreaseAmount()
                        ? request.getDecreaseAmount()
                        : request.getIncrementAmount();
                var response = UpdateExecutorBalanceResponse.newBuilder()
                        .setUpdateExecutorBalanceResponseData(UpdateExecutorBalanceResponseData.newBuilder()
                                .setResult(BALANCE_HAS_BEEN_CHANGED)
                                .setBalance(balance)
                                .build()
                        )
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        };
        return grpcServer(propertyResolver.getIntProperty("grpc.executor-service.port"), controller);
    }

    @Provides
    @Singleton
    @Named("executorServiceWarningServer")
    static Server executorServiceWarningServer(PropertyResolver propertyResolver) {
        var controller = new ExecutorServiceGrpc.ExecutorServiceImplBase() {
            @Override
            public void updateExecutorBalance(UpdateExecutorBalanceRequest request, StreamObserver<UpdateExecutorBalanceResponse> responseObserver) {
                var balance = request.hasDecreaseAmount()
                        ? request.getDecreaseAmount()
                        : request.getIncrementAmount();
                var response = UpdateExecutorBalanceResponse.newBuilder()
                        .setUpdateExecutorBalanceResponseData(UpdateExecutorBalanceResponseData.newBuilder()
                                .setResult(BALANCE_HAS_NOT_BEEN_CHANGED)
                                .setBalance(balance)
                                .build()
                        )
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        };
        return grpcServer(propertyResolver.getIntProperty("grpc.executor-service.port"), controller);
    }

    @Provides
    @Singleton
    @Named("executorServiceErrorServer")
    static Server executorServiceErrorServer(PropertyResolver propertyResolver) {
        var controller = new ExecutorServiceGrpc.ExecutorServiceImplBase() {
            @Override
            public void updateExecutorBalance(UpdateExecutorBalanceRequest request, StreamObserver<UpdateExecutorBalanceResponse> responseObserver) {
                var errorResponse = UpdateExecutorBalanceResponse.newBuilder()
                        .setError(Error.newBuilder()
                                .setCode("code")
                                .setMessage("Message")
                                .build())
                        .build();
                responseObserver.onError(ExceptionHelper.messageToException(errorResponse));
            }
        };
        return grpcServer(propertyResolver.getIntProperty("grpc.executor-service.port"), controller);
    }

    @Provides
    @Singleton
    @Named("customerServiceSuccessServer")
    static Server customerServiceSuccessServer(PropertyResolver propertyResolver) {
        var controller = new CustomerServiceGrpc.CustomerServiceImplBase() {
            @Override
            public void updateCustomerBalance(UpdateCustomerBalanceRequest request, StreamObserver<UpdateCustomerBalanceResponse> responseObserver) {
                var balance = request.hasDecreaseAmount()
                        ? request.getDecreaseAmount()
                        : request.getIncrementAmount();
                var response = UpdateCustomerBalanceResponse.newBuilder()
                        .setUpdateCustomerBalanceResponseData(UpdateCustomerBalanceResponseData.newBuilder()
                                .setResult(ru.craftysoft.orderingsystem.customer.proto.UpdateCustomerBalanceResponseData.Result.BALANCE_HAS_BEEN_CHANGED)
                                .setBalance(balance)
                        )
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        };
        return grpcServer(propertyResolver.getIntProperty("grpc.customer-service.port"), controller);
    }

    @Provides
    @Singleton
    @Named("customerServiceWarningServer")
    static Server customerServiceWarningServer(PropertyResolver propertyResolver) {
        var controller = new CustomerServiceGrpc.CustomerServiceImplBase() {
            @Override
            public void updateCustomerBalance(UpdateCustomerBalanceRequest request, StreamObserver<UpdateCustomerBalanceResponse> responseObserver) {
                var balance = request.hasDecreaseAmount()
                        ? request.getDecreaseAmount()
                        : request.getIncrementAmount();
                var response = UpdateCustomerBalanceResponse.newBuilder()
                        .setUpdateCustomerBalanceResponseData(UpdateCustomerBalanceResponseData.newBuilder()
                                .setResult(ru.craftysoft.orderingsystem.customer.proto.UpdateCustomerBalanceResponseData.Result.BALANCE_HAS_NOT_BEEN_CHANGED)
                                .setBalance(balance)
                        )
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        };
        return grpcServer(propertyResolver.getIntProperty("grpc.customer-service.port"), controller);
    }

    @Provides
    @Singleton
    @Named("customerServiceErrorServer")
    static Server customerServiceErrorServer(PropertyResolver propertyResolver) {
        var controller = new CustomerServiceGrpc.CustomerServiceImplBase() {
            @Override
            public void updateCustomerBalance(UpdateCustomerBalanceRequest request, StreamObserver<UpdateCustomerBalanceResponse> responseObserver) {
                var response = UpdateCustomerBalanceResponse.newBuilder()
                        .setError(ru.craftysoft.orderingsystem.customer.proto.Error.newBuilder()
                                .setCode("code")
                                .setMessage("message")
                                .build()
                        )
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        };
        return grpcServer(propertyResolver.getIntProperty("grpc.customer-service.port"), controller);
    }

    private static Server grpcServer(int port, BindableService service) {
        return ServerBuilder.forPort(port)
                .addService(service)
                .build();
    }

}
