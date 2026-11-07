package com.vlad.managerapp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> EXCLUDED_PREFIXES = Set.of("/styles.css", "/errors/", "/favicon.ico");

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    private final int maxRequestsPerWindow;

    private final long windowMillis;

    private final boolean trustForwardedHeaders;

    public RateLimitFilter(
            @Value("${vlad.security.rate-limit.requests-per-minute:120}") int requestsPerMinute,
            @Value("${vlad.security.rate-limit.window:PT1M}") Duration window,
            @Value("${vlad.security.rate-limit.trust-forwarded-headers:false}") boolean trustForwardedHeaders) {
        this.maxRequestsPerWindow = requestsPerMinute;
        this.windowMillis = window.toMillis();
        this.trustForwardedHeaders = trustForwardedHeaders;
    }

    private record Counter(AtomicInteger count, long windowStart) {
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return EXCLUDED_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        evictExpiredIfNeeded();

        String key = clientKey(request);

        Counter current = this.counters.compute(key, (k, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null || now - existing.windowStart() > this.windowMillis) {
                return new Counter(new AtomicInteger(1), now);
            }
            existing.count().incrementAndGet();
            return existing;
        });

        if (current.count().get() > this.maxRequestsPerWindow) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", Long.toString(this.windowMillis / 1000));
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write("""
                    <!DOCTYPE html>
                    <html lang="ru">
                    <head><meta charset="UTF-8"><title>Слишком много запросов</title></head>
                    <body>
                    <h1>Слишком много запросов</h1>
                    <p>Превышен лимит обращений. Повторите попытку позже.</p>
                    <p><a href="%s/catalogue/products/list">К каталогу</a></p>
                    </body>
                    </html>
                    """.formatted(request.getContextPath()));
            return;
        }

        chain.doFilter(request, response);
    }

    private void evictExpiredIfNeeded() {
        if (this.counters.size() < 10_000) {
            return;
        }
        long now = System.currentTimeMillis();
        this.counters.entrySet().removeIf(entry -> now - entry.getValue().windowStart() > this.windowMillis);

        if (this.counters.size() > 100_000) {
            this.counters.clear();
        }
    }

    private String clientKey(HttpServletRequest request) {
        if (this.trustForwardedHeaders) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
