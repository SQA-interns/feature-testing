package si.konferenca.registration.adapter.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * At most {@code limit} requests per client and endpoint in each clock minute on {@code /api}
 * (SR-03, SB-06); failed organizer logins count too, because this runs before authentication.
 */
public class RateLimitFilter extends OncePerRequestFilter {

  static final long MINUTE_MILLIS = 60_000;
  static final int MAX_TRACKED = 10_000;

  private record Window(long minute, AtomicInteger count) {}

  private final int limit;
  private final Clock clock;
  private final Map<String, Window> windows = new ConcurrentHashMap<>();

  public RateLimitFilter(int limit, Clock clock) {
    this.limit = limit;
    this.clock = clock;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/api/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    long now = clock.millis();
    long minute = now / MINUTE_MILLIS;
    String key =
        ClientAddress.of(request) + ' ' + request.getMethod() + ' ' + request.getRequestURI();
    Window window =
        windows.compute(
            key,
            (k, old) ->
                old == null || old.minute() != minute
                    ? new Window(minute, new AtomicInteger())
                    : old);
    if (windows.size() > MAX_TRACKED) {
      windows.values().removeIf(w -> w.minute() < minute);
    }
    if (window.count().incrementAndGet() > limit) {
      long retryAfter = ((minute + 1) * MINUTE_MILLIS - now + 999) / 1000;
      response.setHeader("Retry-After", String.valueOf(retryAfter));
      ErrorResponses.write(response, 429, "request", "Too many requests. Please try again later.");
      return;
    }
    chain.doFilter(request, response);
  }
}
