package ru.craftysoft.orderingsystem.orderprocessing.dto;

import java.math.BigDecimal;

public record Order(long id,
                    String status,
                    BigDecimal amount,
                    long customerId,
                    Long executorId) {
}
