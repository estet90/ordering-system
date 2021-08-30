package ru.craftysoft.orderingsystem.orderprocessing.error.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.craftysoft.orderingsystem.util.error.code.ExceptionCode;
import ru.craftysoft.orderingsystem.util.error.exception.BusinessException;

@RequiredArgsConstructor
@Getter
public enum BusinessExceptionCode implements ExceptionCode<BusinessException> {

    CUSTOMER_BALANCE_HAS_NOT_BEEN_INCREMENTED("01", "Баланс заказчика не был увеличен"),
    CUSTOMER_BALANCE_HAS_NOT_BEEN_DECREASED("02", "Баланс заказчика не был уменьшен"),
    EXECUTOR_BALANCE_HAS_NOT_BEEN_INCREMENTED("03", "Баланс исполнителя не был увеличен"),
    EXECUTOR_BALANCE_HAS_NOT_BEEN_DECREASED("04", "Баланс исполнителя не был уменьшен"),
    ORDER_HAS_NOT_BEEN_RESERVED("05", "Заказ не был зарезервирован"),
    ORDER_HAS_NOT_BEEN_COMPLETED("06", "Заказ не был завершён"),
    ;

    private final String code;
    private final String message;

}
