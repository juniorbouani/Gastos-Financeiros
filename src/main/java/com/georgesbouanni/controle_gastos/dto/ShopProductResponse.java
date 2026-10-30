package com.georgesbouanni.controle_gastos.dto;

import java.math.BigDecimal;

public record ShopProductResponse(
        String id, String nome, BigDecimal preco, String imagem, String pagina) {}
