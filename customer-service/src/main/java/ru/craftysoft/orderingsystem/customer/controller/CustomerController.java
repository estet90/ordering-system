package ru.craftysoft.orderingsystem.customer.controller;

import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.customer.builder.operation.GetCustomerResponseBuilder;
import ru.craftysoft.orderingsystem.customer.builder.operation.UpdateCustomerBalanceResponseBuilder;
import ru.craftysoft.orderingsystem.customer.error.operation.ModuleOperationCode;
import ru.craftysoft.orderingsystem.customer.logic.GetCustomerOperation;
import ru.craftysoft.orderingsystem.customer.logic.UpdateCustomerBalanceOperation;
import ru.craftysoft.orderingsystem.customer.proto.*;
import ru.craftysoft.orderingsystem.util.grpc.ExceptionHelper;

import javax.inject.Inject;
import javax.inject.Singleton;

import static ru.craftysoft.orderingsystem.customer.error.operation.ModuleOperationCode.GET_CUSTOMER;
import static ru.craftysoft.orderingsystem.customer.error.operation.ModuleOperationCode.UPDATE_CUSTOMER_BALANCE;
import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.mapException;
import static ru.craftysoft.orderingsystem.util.error.logging.ExceptionLoggerHelper.logError;
import static ru.craftysoft.orderingsystem.util.mdc.MdcKey.OPERATION_NAME_KEY;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withContext;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

@Singleton
@Slf4j
public class CustomerController extends CustomerServiceGrpc.CustomerServiceImplBase {

    private final GetCustomerOperation getCustomerOperation;
    private final UpdateCustomerBalanceOperation updateCustomerBalanceOperation;
    private final GetCustomerResponseBuilder getCustomerResponseBuilder;
    private final UpdateCustomerBalanceResponseBuilder updateCustomerBalanceResponseBuilder;

    @Inject
    public CustomerController(GetCustomerOperation getCustomerOperation,
                              UpdateCustomerBalanceOperation updateCustomerBalanceOperation,
                              GetCustomerResponseBuilder getCustomerResponseBuilder,
                              UpdateCustomerBalanceResponseBuilder updateCustomerBalanceResponseBuilder) {
        this.getCustomerOperation = getCustomerOperation;
        this.updateCustomerBalanceOperation = updateCustomerBalanceOperation;
        this.getCustomerResponseBuilder = getCustomerResponseBuilder;
        this.updateCustomerBalanceResponseBuilder = updateCustomerBalanceResponseBuilder;
    }

    @Override
    public void getCustomer(GetCustomerRequest request, StreamObserver<GetCustomerResponse> responseObserver) {
        var context = Context.current().withValue(OPERATION_NAME_KEY, GET_CUSTOMER.name());
        withContext(context, () -> getCustomerOperation.process(request)).whenComplete(withMdc((response, throwable) -> {
            if (throwable == null) {
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                var baseException = mapException(throwable, ModuleOperationCode::resolve);
                logError(log, "CustomerController.getCustomer", baseException);
                var errorResponse = getCustomerResponseBuilder.build(baseException);
                responseObserver.onError(ExceptionHelper.messageToException(errorResponse));
            }
        }));
    }

    @Override
    public void updateCustomerBalance(UpdateCustomerBalanceRequest request, StreamObserver<UpdateCustomerBalanceResponse> responseObserver) {
        var context = Context.current().withValue(OPERATION_NAME_KEY, UPDATE_CUSTOMER_BALANCE.name());
        withContext(context, () -> updateCustomerBalanceOperation.process(request)).whenComplete(withMdc((response, throwable) -> {
            if (throwable == null) {
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                var baseException = mapException(throwable, ModuleOperationCode::resolve);
                logError(log, "CustomerController.updateCustomerBalance", baseException);
                var errorResponse = updateCustomerBalanceResponseBuilder.build(baseException);
                responseObserver.onError(ExceptionHelper.messageToException(errorResponse));
            }
        }));
    }
}
