package ru.craftysoft.orderingsystem.customer.service.dao;

import io.grpc.Context;
import ru.craftysoft.orderingsystem.customer.dto.Customer;
import ru.craftysoft.orderingsystem.customer.proto.GetCustomerRequest;
import ru.craftysoft.orderingsystem.customer.proto.UpdateCustomerBalanceRequest;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withContext;
import static ru.craftysoft.orderingsystem.util.proto.ProtoUtils.moneyToBigDecimal;

@Singleton
public class CustomerDaoAdapter {

    private final CustomerDao dao;
    private final Executor dbExecutor;

    @Inject
    public CustomerDaoAdapter(CustomerDao dao,
                              @Named("dbExecutor") Executor dbExecutor) {
        this.dao = dao;
        this.dbExecutor = dbExecutor;
    }

    public CompletableFuture<Customer> getCustomer(GetCustomerRequest request) {
        var context = Context.current();
        Supplier<Customer> callback = request.hasId()
                ? () -> dao.getCustomerById(request.getId())
                : () -> dao.getCustomerByUserId(request.getUserId());
        return CompletableFuture.supplyAsync(
                () -> withContext(context, callback),
                dbExecutor
        );
    }

    public CompletableFuture<BigDecimal> updateCustomerBalance(UpdateCustomerBalanceRequest request) {
        var context = Context.current();
        Supplier<BigDecimal> callback = request.hasIncrementAmount()
                ? () -> dao.incrementAmount(request.getId(), moneyToBigDecimal(request.getIncrementAmount()))
                : () -> dao.decreaseAmount(request.getId(), moneyToBigDecimal(request.getDecreaseAmount()));
        return CompletableFuture.supplyAsync(
                () -> withContext(context, callback),
                dbExecutor
        );
    }
}
