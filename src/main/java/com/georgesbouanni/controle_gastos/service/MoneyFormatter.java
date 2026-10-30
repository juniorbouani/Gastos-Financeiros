package com.georgesbouanni.controle_gastos.service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class MoneyFormatter {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private MoneyFormatter() {}

    public static String format(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(PT_BR)
                .format(value)
                .replaceAll("[\\u00a0\\u202f]", " ");
    }
}
