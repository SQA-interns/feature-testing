package si.konferenca.registration.security;

import java.time.Clock;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import si.konferenca.registration.infrastructure.config.AppSettings;

/**
 * Servlet filters that run before Spring Security: rate limit first, then body size, then the
 * HTTPS-only rule for organizer requests; plus the optional local-development CORS origin.
 */
@Configuration
public class SecurityFilters {

  /** Order of Spring Security's filter chain (Spring Boot default). */
  private static final int BEFORE_SECURITY = -100;

  @Bean
  public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(
      AppSettings settings, Clock clock) {
    return register(
        new RateLimitFilter(settings.rateLimitPerMinute(), clock), BEFORE_SECURITY - 30);
  }

  @Bean
  public FilterRegistrationBean<RequestSizeFilter> requestSizeFilter(AppSettings settings) {
    return register(new RequestSizeFilter(settings.requestMaxBytes()), BEFORE_SECURITY - 20);
  }

  @Bean
  public FilterRegistrationBean<HttpsOnlyFilter> httpsOnlyFilter(AppSettings settings) {
    return register(new HttpsOnlyFilter(settings.organizer().httpsOnly()), BEFORE_SECURITY - 10);
  }

  private static <F extends jakarta.servlet.Filter> FilterRegistrationBean<F> register(
      F filter, int order) {
    FilterRegistrationBean<F> registration = new FilterRegistrationBean<>(filter);
    registration.setOrder(order);
    return registration;
  }

  /** CORS only when CORS_ALLOWED_ORIGIN is set (local development); never in production. */
  @Bean
  public CorsConfigurationSource corsConfigurationSource(AppSettings settings) {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    String origin = settings.corsAllowedOrigin();
    if (origin != null && !origin.isBlank()) {
      CorsConfiguration cors = new CorsConfiguration();
      cors.addAllowedOrigin(origin);
      cors.addAllowedMethod("GET");
      cors.addAllowedMethod("POST");
      cors.addAllowedHeader("Content-Type");
      cors.addAllowedHeader("Authorization");
      cors.addAllowedHeader("X-Requested-With");
      source.registerCorsConfiguration("/api/**", cors);
    }
    return source;
  }
}
