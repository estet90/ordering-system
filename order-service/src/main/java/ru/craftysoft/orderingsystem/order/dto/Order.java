package ru.craftysoft.orderingsystem.order.dto;

import java.math.BigDecimal;

public record Order(long id,
                    String name,
                    BigDecimal price,
                    long customerId) {
}
