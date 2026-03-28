package com.rehabiapp.api.infrastructure.config;

import com.rehabiapp.api.infrastructure.audit.AuditReadInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración MVC — registro de interceptores de la capa de presentación.
 *
 * <p>Registra el AuditReadInterceptor para los endpoints de pacientes,
 * garantizando el registro de accesos de lectura a datos clínicos
 * (Ley 41/2002, ENS Alto — trazabilidad completa).</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuditReadInterceptor auditReadInterceptor;

    public WebMvcConfig(AuditReadInterceptor auditReadInterceptor) {
        this.auditReadInterceptor = auditReadInterceptor;
    }

    /**
     * Registra el interceptor de auditoría de lecturas para todos los paths de pacientes.
     * El interceptor se aplica a /api/pacientes/** incluyendo las asignaciones del paciente.
     *
     * @param registry Registro de interceptores de Spring MVC.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(auditReadInterceptor)
                .addPathPatterns("/api/pacientes/**");
    }
}
