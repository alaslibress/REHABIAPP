package com.rehabiapp.api.infrastructure.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que rechaza con 413 los payloads JSON superiores a 1 MB.
 *
 * <p>Los uploads multipart (PDF de tratamiento, foto de paciente) se ignoran
 * porque su limite se controla a traves de las propiedades
 * {@code spring.servlet.multipart.*}.</p>
 */
@Component
@Order(2)
public class PayloadSizeFilter extends OncePerRequestFilter {

    private static final long MAX_JSON_BYTES = 1L * 1024L * 1024L;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String contentType = request.getContentType();
        long contentLength = request.getContentLengthLong();

        if (contentType != null
                && contentType.toLowerCase().startsWith("application/json")
                && contentLength > MAX_JSON_BYTES) {
            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Payload demasiado grande\",\"maxBytes\":"
                            + MAX_JSON_BYTES + "}");
            return;
        }
        chain.doFilter(request, response);
    }
}
