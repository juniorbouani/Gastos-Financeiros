package com.georgesbouanni.controle_gastos.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Valida o token antes de permitir que uma requisição acesse os controllers protegidos. */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailService userDetailService;

    @Autowired
    public JwtAuthenticationFilter(
            JwtService jwtService, CustomUserDetailService userDetailService) {
        this.jwtService = jwtService;
        this.userDetailService = userDetailService;
    }

    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);
            String cpf = jwtService.extractCpf(token);
            if (cpf == null || !jwtService.isTokenValid(token, cpf)) {
                unauthorized(response);
                return;
            }
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailService.loadUserByUsername(cpf);
                if (!userDetails.isEnabled()) {
                    unauthorized(response);
                    return;
                }
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (io.jsonwebtoken.JwtException
                | IllegalArgumentException
                | org.springframework.security.core.AuthenticationException ex) {
            unauthorized(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter()
                .write("{\"message\":\"Sessão inválida ou expirada. Entre novamente.\"}");
    }
}
