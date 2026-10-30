package com.georgesbouanni.controle_gastos.dto;

import java.math.BigDecimal;

public record CategorySpent(Long categoryId, String categoryName, BigDecimal total, Long count) {}
