package ru.craftysoft.orderingsystem.order.service.dao;

import com.google.type.Money;
import ru.craftysoft.orderingsystem.order.dto.AddedOrder;
import ru.craftysoft.orderingsystem.order.dto.Order;
import ru.craftysoft.orderingsystem.order.proto.AddOrderRequest;
import ru.craftysoft.orderingsystem.order.proto.ReserveOrderRequest;
import ru.craftysoft.orderingsystem.util.properties.PropertyResolver;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

@Singleton
public class OrderDaoAdapter {

    private final OrderDao dao;
    private final Executor dbExecutor;
    private final String statusActive;
    private final String statusUnavailable;
    private final String statusReserved;

    @Inject
    public OrderDaoAdapter(OrderDao dao,
                           @Named("dbExecutor") Executor dbExecutor,
                           PropertyResolver propertyResolver) {
        this.dao = dao;
        this.dbExecutor = dbExecutor;
        this.statusActive = propertyResolver.getStringProperty("db.query-parameter.orders.order-status.active");
        this.statusUnavailable = propertyResolver.getStringProperty("db.query-parameter.orders.order-status.unavailable");
        this.statusReserved = propertyResolver.getStringProperty("db.query-parameter.orders.order-status.reserved");
    }

    public CompletableFuture<List<Order>> getOrders() {
        return CompletableFuture.supplyAsync(
                withMdc(() -> dao.getOrders(this.statusActive)),
                dbExecutor
        );
    }

    public CompletableFuture<Long> addOrder(AddOrderRequest request) {
        var price = request.getPrice();
        var balance = request.getCustomer().getBalance();
        var status = resolveStatus(price, balance);
        var addedOrder = new AddedOrder(
                request.getName(),
                new BigDecimal(price.toString()),
                request.getCustomer().getId(),
                status
        );
        return CompletableFuture.supplyAsync(
                withMdc(() -> dao.addOrder(addedOrder)),
                dbExecutor
        );
    }

    private String resolveStatus(Money price, Money balance) {
        if (price.getUnits() < balance.getUnits()) {
            return statusActive;
        } else if (price.getUnits() == balance.getUnits()) {
            return price.getNanos() <= balance.getNanos()
                    ? statusActive
                    : statusUnavailable;
        }
        return statusUnavailable;
    }

    public CompletableFuture<Integer> reserveOrder(ReserveOrderRequest request) {
        return CompletableFuture.supplyAsync(
                withMdc(() -> dao.updateOrderStatus(
                        request.getId(),
                        request.getExecutorId(),
                        statusActive,
                        statusReserved
                )),
                dbExecutor
        );
    }

}
