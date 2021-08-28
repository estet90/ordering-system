package ru.craftysoft.orderingsystem.customer.dto;

import javax.annotation.Nonnull;
import java.math.BigDecimal;

public record Customer(long id, @Nonnull BigDecimal balance) {
}
