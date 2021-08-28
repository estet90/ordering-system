package ru.craftysoft.orderingsystem.executor.dto;

import java.math.BigDecimal;

public record Executor(long id, BigDecimal balance) {
}
