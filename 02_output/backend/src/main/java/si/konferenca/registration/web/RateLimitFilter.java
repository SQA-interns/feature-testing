package si.konferenca.registration.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import si.konferenca.registration.config.AppProperties;

/**
 * Fixed one-minute windows per client address and endpoint group (SR-03, SB-06). Runs before
 * security, so failed export logins count too.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

  private static final long WINDOW_MS = 60_000;

  private final AppProperties.RateLimit limits;
  private final Clock clock;
  private final Map<String, Window> windows = new ConcurrentHashMap<>();
  private volatile long sweptWindow;

  public RateLimitFilter(AppProperties properties, Clock clock) {
    this.limits = properties.rateLimit();
    this.clock = clock;
  }

  private record Window(long index, AtomicInteger count) {}

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/api/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    long now = clock.millis();
    long index = now / WINDOW_MS;
    sweep(index);
    String group = group(request);
    String key = group + '|' + request.getRemoteAddr();
    Window window =
        windows.compute(
            key,
            (k, w) -> w == null || w.index() != index ? new Window(index, new AtomicInteger()) : w);
    if (window.count().incrementAndGet() > limit(group)) {
      long retryAfter = ((index + 1) * WINDOW_MS - now + 999) / 1000;
      response.setHeader("Retry-After", String.valueOf(retryAfter));
      JsonErrors.write(response, 429, "Too many requests. Please try again later.");
      return;
    }
    chain.doFilter(request, response);
  }

  private static String group(HttpServletRequest request) {
    String uri = request.getRequestURI();
    if ("/api/form-config".equals(uri)) {
      return "form-config";
    }
    if (HttpsOnlyFilter.EXPORT_PATH.equals(uri)) {
      return "export";
    }
    return "registration";
  }

  private int limit(String group) {
    return switch (group) {
      case "form-config" -> limits.formConfigPerMinute();
      case "export" -> limits.exportPerMinute();
      default -> limits.registrationPerMinute();
    };
  }

  /** Drops windows of earlier minutes, once per minute. */
  private void sweep(long index) {
    if (sweptWindow != index) {
      sweptWindow = index;
      windows.values().removeIf(w -> w.index() < index);
    }
  }
}
