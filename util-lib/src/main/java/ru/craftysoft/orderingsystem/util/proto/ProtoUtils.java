package ru.craftysoft.orderingsystem.util.proto;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import com.google.protobuf.util.JsonFormat;
import com.google.type.Money;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class ProtoUtils {

    public static Money bigDecimalToMoney(BigDecimal value) {
        var scaledValue = value.setScale(2, RoundingMode.HALF_UP);
        var units = scaledValue.longValue();
        var nanos = (scaledValue.subtract(new BigDecimal(units)))
                .multiply(BigDecimal.valueOf(1000_000_000))
                .intValue();
        return Money.newBuilder()
                .setUnits(units)
                .setNanos(nanos)
                .build();
    }

    public static BigDecimal moneyToBigDecimal(Money value) {
        return (new BigDecimal(value.getUnits())
                .add(new BigDecimal(value.getNanos()).divide(new BigDecimal(1000_000_000), MathContext.DECIMAL32)))
                .setScale(2, RoundingMode.HALF_DOWN);
    }

    @SneakyThrows
    public static <T extends GeneratedMessageV3> String toPrettyString(T entity) {
        return JsonFormat.printer().print(entity);
    }

    public static StringValue buildStringValue(String s) {
        return ofNullable(s)
                .map(value -> StringValue.newBuilder().setValue(value).build())
                .orElseGet(StringValue.newBuilder()::getDefaultInstanceForType);
    }

    public static Int64Value buildInt64Value(Long l) {
        return ofNullable(l)
                .map(value -> Int64Value.newBuilder().setValue(value).build())
                .orElseGet(Int64Value.newBuilder()::getDefaultInstanceForType);
    }

}
