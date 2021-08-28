package ru.craftysoft.orderingsystem.gateway.service.grpc;

import io.grpc.stub.StreamObserver;
import ru.craftysoft.orderingsystem.order.proto.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

@Singleton
public class OrderServiceClient {

    private final OrderServiceGrpc.OrderServiceStub orderServiceStub;

    @Inject
    public OrderServiceClient(OrderServiceGrpc.OrderServiceStub orderServiceStub) {
        this.orderServiceStub = orderServiceStub;
    }

    public CompletableFuture<GetOrdersResponse> getOrders(GetOrdersRequest request) {
        var result = new CompletableFuture<GetOrdersResponse>();
        orderServiceStub.getOrders(request, new StreamObserver<>() {
            private GetOrdersResponse response;

            @Override
            public void onNext(GetOrdersResponse response) {
                this.response = response;
            }

            @Override
            public void onError(Throwable throwable) {
                result.completeExceptionally(throwable);
            }

            @Override
            public void onCompleted() {
                result.complete(this.response);
            }
        });
        return result;
    }

    public CompletableFuture<AddOrderResponse> addOrder(AddOrderRequest request) {
        var result = new CompletableFuture<AddOrderResponse>();
        orderServiceStub.addOrder(request, new StreamObserver<>() {
            private AddOrderResponse response;

            @Override
            public void onNext(AddOrderResponse response) {
                this.response = response;
            }

            @Override
            public void onError(Throwable throwable) {
                result.completeExceptionally(throwable);
            }

            @Override
            public void onCompleted() {
                result.complete(this.response);
            }
        });
        return result;
    }

    public CompletableFuture<ReserveOrderResponse> reserveOrder(ReserveOrderRequest request) {
        var result = new CompletableFuture<ReserveOrderResponse>();
        orderServiceStub.reserveOrder(request, new StreamObserver<>() {
            private ReserveOrderResponse response;

            @Override
            public void onNext(ReserveOrderResponse response) {
                this.response = response;
            }

            @Override
            public void onError(Throwable throwable) {
                result.completeExceptionally(throwable);
            }

            @Override
            public void onCompleted() {
                result.complete(this.response);
            }
        });
        return result;
    }
}
