package ru.craftysoft.orderingsystem.orderprocessing.dto;

import java.math.BigDecimal;

public record Order(long id,
                    BigDecimal price,
                    long consumerId,
                    long executorId) {
}
