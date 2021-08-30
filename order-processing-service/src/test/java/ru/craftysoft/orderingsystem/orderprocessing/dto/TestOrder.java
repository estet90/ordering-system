package ru.craftysoft.orderingsystem.orderprocessing.dto;

import java.math.BigDecimal;

public record TestOrder(long id,
                        String status,
                        BigDecimal price,
                        long customerId,
                        Long executorId) {
}
