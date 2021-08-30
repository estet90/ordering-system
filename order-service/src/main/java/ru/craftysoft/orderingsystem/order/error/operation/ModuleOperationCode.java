package ru.craftysoft.orderingsystem.order.error.operation;

import io.grpc.Context;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import ru.craftysoft.orderingsystem.util.error.operation.OperationCode;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static ru.craftysoft.orderingsystem.util.mdc.MdcKey.OPERATION_NAME;
import static ru.craftysoft.orderingsystem.util.mdc.MdcKey.OPERATION_NAME_KEY;

@Getter
@RequiredArgsConstructor
public enum ModuleOperationCode implements OperationCode {

    ADD_ORDER("01"),
    GET_ORDERS("02"),
    RESERVE_ORDER("03"),
    ;

    private final String code;

    private static final Map<String, OperationCode> valuesByName = Stream.of(ModuleOperationCode.values())
            .collect(Collectors.toMap(ModuleOperationCode::name, Function.identity()));

    public static OperationCode resolve() {
        return ofNullable(valuesByName.get(MDC.get(OPERATION_NAME))).orElse(() -> OTHER);
    }

    public static OperationCode resolve(Context context) {
        return ofNullable(valuesByName.get(OPERATION_NAME_KEY.get(context))).orElse(() -> OTHER);
    }

}
