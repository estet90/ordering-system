package ru.craftysoft.orderingsystem.util.proto;

import com.google.type.Money;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

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

}
