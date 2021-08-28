package ru.craftysoft.orderingsystem.order.dto;

import javax.annotation.Nonnull;
import java.math.BigDecimal;

public record Order(long id,
                    @Nonnull String name,
                    @Nonnull BigDecimal price,
                    long customerId) {
}
