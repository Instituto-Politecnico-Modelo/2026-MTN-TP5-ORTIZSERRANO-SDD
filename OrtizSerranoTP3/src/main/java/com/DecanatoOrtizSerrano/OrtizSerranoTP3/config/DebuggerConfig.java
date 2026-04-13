package com.DecanatoOrtizSerrano.OrtizSerranoTP3.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Configuración del Debugger HTTP
 * Intercepta todas las peticiones HTTP y las registra para debugging
 */
@Configuration
public class DebuggerConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(DebuggerConfig.class);

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HttpDebuggerInterceptor());
    }

    /**
     * Interceptor para debugging de peticiones HTTP
     */
    public static class HttpDebuggerInterceptor implements HandlerInterceptor {
        private static final Logger log = LoggerFactory.getLogger(HttpDebuggerInterceptor.class);

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            log.debug("════════════════════════════════════════════════════════════");
            log.debug("🔵 NUEVA PETICIÓN HTTP");
            log.debug("Método: {}", request.getMethod());
            log.debug("URI: {}", request.getRequestURI());
            log.debug("Query String: {}", request.getQueryString());
            log.debug("Remote Address: {}", request.getRemoteAddr());
            log.debug("Session ID: {}", request.getSession(false) != null ? request.getSession().getId() : "Sin sesión");
            
            // Headers
            log.debug("--- Headers ---");
            request.getHeaderNames().asIterator().forEachRemaining(headerName -> 
                log.debug("{}: {}", headerName, request.getHeader(headerName))
            );
            
            // Parámetros
            if (!request.getParameterMap().isEmpty()) {
                log.debug("--- Parámetros ---");
                request.getParameterMap().forEach((key, values) -> 
                    log.debug("{}: {}", key, String.join(", ", values))
                );
            }
            
            log.debug("Handler: {}", handler);
            log.debug("════════════════════════════════════════════════════════════");
            
            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                   Object handler, Exception ex) {
            log.debug("────────────────────────────────────────────────────────────");
            log.debug("🟢 RESPUESTA HTTP");
            log.debug("Status: {}", response.getStatus());
            log.debug("Content Type: {}", response.getContentType());
            
            if (ex != null) {
                log.error("❌ EXCEPCIÓN: ", ex);
            }
            
            log.debug("────────────────────────────────────────────────────────────");
        }
    }
}
