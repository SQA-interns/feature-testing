package si.konferenca.registration.config;

import java.time.Clock;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import si.konferenca.registration.adapter.web.BodySizeLimitFilter;
import si.konferenca.registration.adapter.web.RateLimitFilter;

/** Registers the SR-03 filters ahead of Spring Security for every {@code /api/*} request. */
@Configuration
public class WebFilterConfiguration {

  @Bean
  FilterRegistrationBean<RateLimitFilter> rateLimitFilter(AppProperties properties, Clock clock) {
    FilterRegistrationBean<RateLimitFilter> registration =
        new FilterRegistrationBean<>(
            new RateLimitFilter(properties.rateLimit().perMinute(), clock));
    registration.addUrlPatterns("/api/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
    return registration;
  }

  @Bean
  FilterRegistrationBean<BodySizeLimitFilter> bodySizeLimitFilter(AppProperties properties) {
    FilterRegistrationBean<BodySizeLimitFilter> registration =
        new FilterRegistrationBean<>(new BodySizeLimitFilter(properties.request().maxBytes()));
    registration.addUrlPatterns("/api/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
    return registration;
  }
}
