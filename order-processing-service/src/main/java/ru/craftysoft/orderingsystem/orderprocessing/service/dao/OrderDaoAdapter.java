package ru.craftysoft.orderingsystem.orderprocessing.service.dao;

import ru.craftysoft.orderingsystem.orderprocessing.dto.Order;
import ru.craftysoft.orderingsystem.orderprocessing.proto.CompleteOrderRequest;
import ru.craftysoft.orderingsystem.orderprocessing.proto.ReserveOrderRequest;
import ru.craftysoft.orderingsystem.util.properties.PropertyResolver;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static ru.craftysoft.orderingsystem.orderprocessing.error.exception.BusinessExceptionCode.ORDER_HAS_NOT_BEEN_COMPLETED;
import static ru.craftysoft.orderingsystem.orderprocessing.error.exception.BusinessExceptionCode.ORDER_HAS_NOT_BEEN_RESERVED;
import static ru.craftysoft.orderingsystem.orderprocessing.error.operation.ModuleOperationCode.resolve;
import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.newBusinessException;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;
import static ru.craftysoft.orderingsystem.util.proto.ProtoUtils.moneyToBigDecimal;

@Singleton
public class OrderDaoAdapter {

    private final OrderDao dao;
    private final Executor dbExecutor;
    private final String statusReserved;
    private final String statusInProcessing;
    private final int limit;

    @Inject
    public OrderDaoAdapter(OrderDao dao,
                           @Named("dbExecutor") Executor dbExecutor,
                           PropertyResolver propertyResolver) {
        this.dao = dao;
        this.dbExecutor = dbExecutor;
        this.statusReserved = propertyResolver.getStringProperty("db.query-parameter.orders.order-status.reserved");
        this.statusInProcessing = propertyResolver.getStringProperty("db.query-parameter.orders.order-status.in-processing");
        this.limit = 10;
    }

    public List<Order> processOrders() {
        return dao.processOrders(statusReserved, statusInProcessing, limit);
    }

    public CompletableFuture<Void> reserveOrder(Order order) {
        return CompletableFuture.supplyAsync(withMdc(() -> dao.updateOrderStatus(order.id(), statusReserved)), dbExecutor)
                .thenAccept(withMdc(count -> {
                    if (count == 0) {
                        throw newBusinessException(resolve(), ORDER_HAS_NOT_BEEN_RESERVED, "id='%s'".formatted(order.id()));
                    }
                }));

    }

    public CompletableFuture<Void> reserveOrder(ReserveOrderRequest request) {
        return CompletableFuture.supplyAsync(withMdc(() -> dao.updateOrderStatus(request.getOrderId(), statusReserved)), dbExecutor)
                .thenAccept(withMdc(count -> {
                    if (count == 0) {
                        throw newBusinessException(resolve(), ORDER_HAS_NOT_BEEN_RESERVED, "id='%s'".formatted(request.getOrderId()));
                    }
                }));
    }

    public CompletableFuture<Void> completeOrder(CompleteOrderRequest request) {
        return CompletableFuture.supplyAsync(withMdc(() -> dao.completeOrder(request.getOrderId(), request.getCustomerId(), moneyToBigDecimal(request.getCustomerBalance()))), dbExecutor)
                .thenAccept(withMdc(count -> {
                    if (count == 0) {
                        throw newBusinessException(resolve(), ORDER_HAS_NOT_BEEN_COMPLETED, "id='%s'".formatted(request.getOrderId()));
                    }
                }));
    }
}
