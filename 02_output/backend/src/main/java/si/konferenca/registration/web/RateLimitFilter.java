package si.konferenca.registration.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Fixed one-minute window per client address and endpoint for every {@code /api/} request,
 * including failed organizer logins; over the limit the answer is HTTP 429 (SR-03, SB-06).
 */
public class RateLimitFilter extends OncePerRequestFilter {

  static final String BODY = "{\"message\":\"Too many requests. Please try again later.\"}";
  private static final long MINUTE_MILLIS = 60_000;
  private static final int MAX_TRACKED = 10_000;

  private record Window(long minute, AtomicInteger count) {}

  private final int perMinute;
  private final Clock clock;
  private final Map<String, Window> windows = new ConcurrentHashMap<>();

  public RateLimitFilter(int perMinute, Clock clock) {
    this.perMinute = perMinute;
    this.clock = clock;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith(request.getContextPath() + "/api/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    long now = clock.millis();
    long minute = now / MINUTE_MILLIS;
    String key =
        request.getRemoteAddr() + ' ' + request.getMethod() + ' ' + request.getRequestURI();
    Window window =
        windows.compute(
            key,
            (k, old) ->
                old == null || old.minute() != minute
                    ? new Window(minute, new AtomicInteger())
                    : old);
    if (windows.size() > MAX_TRACKED) {
      windows.values().removeIf(w -> w.minute() != minute);
    }
    if (window.count().incrementAndGet() > perMinute) {
      long retryAfter = Math.max(1, ((minute + 1) * MINUTE_MILLIS - now + 999) / 1000);
      response.setStatus(429);
      response.setHeader("Retry-After", String.valueOf(retryAfter));
      response.setContentType("application/json;charset=UTF-8");
      response.getOutputStream().write(BODY.getBytes(StandardCharsets.UTF_8));
      return;
    }
    chain.doFilter(request, response);
  }
}
