package com.georgesbouanni.controle_gastos.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public OpenAPI apiInformation() {
        return new OpenAPI()
                .info(new Info()
                        .title("Controle de gastos")
                        .version("1.0.0")
                        .description("API REST para gerenciar as transações financeiras pessoais."));
    }
}
