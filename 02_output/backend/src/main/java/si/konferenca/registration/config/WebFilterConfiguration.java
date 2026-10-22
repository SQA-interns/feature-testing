package si.konferenca.registration.config;

import jakarta.servlet.Filter;
import java.time.Clock;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import si.konferenca.registration.adapter.web.HttpsOnlyFilter;
import si.konferenca.registration.adapter.web.RateLimitFilter;
import si.konferenca.registration.adapter.web.RequestSizeFilter;

/** Size limit, rate limit and HTTPS-only check, all before Spring Security (order -100). */
@Configuration
public class WebFilterConfiguration {

  @Bean
  FilterRegistrationBean<RequestSizeFilter> requestSizeFilter(AppSettings settings) {
    return registration(new RequestSizeFilter(settings.maxRequestBytes()), -110);
  }

  @Bean
  FilterRegistrationBean<RateLimitFilter> rateLimitFilter(AppSettings settings, Clock clock) {
    return registration(new RateLimitFilter(settings.rateLimitPerMinute(), clock), -105);
  }

  @Bean
  FilterRegistrationBean<HttpsOnlyFilter> httpsOnlyFilter(AppSettings settings) {
    return registration(new HttpsOnlyFilter(settings.organizer().httpsOnly()), -102);
  }

  private static <T extends Filter> FilterRegistrationBean<T> registration(T filter, int order) {
    FilterRegistrationBean<T> registration = new FilterRegistrationBean<>(filter);
    registration.setOrder(order);
    return registration;
  }
}
