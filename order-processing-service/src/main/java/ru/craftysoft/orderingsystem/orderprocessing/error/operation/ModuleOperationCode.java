package ru.craftysoft.orderingsystem.orderprocessing.error.operation;

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

@Getter
@RequiredArgsConstructor
public enum ModuleOperationCode implements OperationCode {

    EXTRACT_ORDER("01"),
    DECREASE_CUSTOMER_AMOUNT("02"),
    INCREMENT_EXECUTOR_AMOUNT("03"),
    COMPLETE_ORDER("04"),
    DECREASE_EXECUTOR_AMOUNT("05"),
    INCREMENT_CUSTOMER_AMOUNT("06"),
    RESERVE_ORDER("07"),
    ;

    private final String code;

    private static final Map<String, OperationCode> valuesByName = Stream.of(ModuleOperationCode.values())
            .collect(Collectors.toMap(ModuleOperationCode::name, Function.identity()));

    public static OperationCode resolve() {
        return ofNullable(valuesByName.get(MDC.get(OPERATION_NAME))).orElse(() -> OTHER);
    }

}
