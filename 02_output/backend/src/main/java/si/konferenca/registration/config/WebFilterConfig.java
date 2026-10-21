package si.konferenca.registration.config;

import java.time.Clock;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import si.konferenca.registration.web.RateLimitFilter;
import si.konferenca.registration.web.RequestSizeFilter;

/** Registers the rate and size limits ahead of Spring Security (SR-03). */
@Configuration
public class WebFilterConfig {

  @Bean
  public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(
      AppProperties properties, Clock clock) {
    FilterRegistrationBean<RateLimitFilter> registration =
        new FilterRegistrationBean<>(
            new RateLimitFilter(properties.rateLimit().perMinute(), clock));
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
    return registration;
  }

  @Bean
  public FilterRegistrationBean<RequestSizeFilter> requestSizeFilter(AppProperties properties) {
    FilterRegistrationBean<RequestSizeFilter> registration =
        new FilterRegistrationBean<>(new RequestSizeFilter(properties.request().maxBytes()));
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
    return registration;
  }
}
