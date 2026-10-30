package com.georgesbouanni.controle_gastos.config;

import com.georgesbouanni.controle_gastos.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Recursos da interface: as APIs continuam exigindo autenticação.
                        .requestMatchers(HttpMethod.GET, "/", "/index.html", "/css/**", "/js/login.js", "/img/**",
                                "/PaginaInicial/paginainicial.html", "/js/main.js",
                                "/cadastro/cadastro.html", "/js/cadastro.js", "/js/paginainicial.js",
                                "/pagamentos/pagar/extrato.html", "/js/extrato.js",
                                "/pagamentos/pix.html", "/pagamentos/criarchave.html",
                                "/pagamentos/transferir.html", "/pagamentos/confirmar.html",
                                "/pagamentos/pixcopiaecola.html", "/js/pix.js", "/cartoes/cartoes.html",
                                "/cartoes/solicitarCartao.html", "/js/cartoes.js",
                                "/pagamentos/pagar/pagar.html", "/pagamentos/pagar/lercodigo.html",
                                "/pagamentos/pagar/transferirPagar.html", "/js/pagamentos.js",
                                "/perfil/perfil.html", "/perfil/dadoscadastrais.html",
                                "/configuracoes/configuracoes.html", "/js/perfil.js",
                                "/seguranca/seguranca.html", "/encerrarConta/encerrarConta.html",
                                "/notificacoes/notificacoes.html", "/categorias/categorias.html",
                                "/orcamentos/orcamentos.html", "/js/conta-painel.js").permitAll()
                        .requestMatchers(HttpMethod.POST, "/users").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
