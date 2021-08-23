package ru.craftysoft.orderingsystem.order.service.dao;

import io.grpc.Context;
import ru.craftysoft.orderingsystem.order.dto.Order;
import ru.craftysoft.orderingsystem.util.properties.PropertyResolver;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withContext;

@Singleton
public class OrderDaoAdapter {

    private final OrderDao dao;
    private final Executor dbExecutor;
    private final String status;

    @Inject
    public OrderDaoAdapter(OrderDao dao,
                           @Named("dbExecutor") Executor dbExecutor,
                           PropertyResolver propertyResolver) {
        this.dao = dao;
        this.dbExecutor = dbExecutor;
        this.status = propertyResolver.getStringProperty("db.query-parameter.orders.order-status");
    }

    public CompletableFuture<List<Order>> getOrders() {
        var context = Context.current();
        return CompletableFuture.supplyAsync(
                () -> withContext(context, () -> dao.getOrders(this.status)),
                dbExecutor
        );
    }

}
