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
 * SR-03, SB-06: at most {@code perMinute} requests per client address and endpoint (method and
 * path) in each clock minute; more are answered with 429. Runs before authentication, so failed
 * logins count too.
 */
public class RateLimitFilter extends OncePerRequestFilter {

  static final String BODY = "{\"message\":\"Too many requests. Please try again later.\"}";
  static final int MAX_TRACKED_CLIENTS = 10_000;

  private final int perMinute;
  private final Clock clock;
  private final Map<String, Window> windows = new ConcurrentHashMap<>();

  public RateLimitFilter(int perMinute, Clock clock) {
    this.perMinute = perMinute;
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
    long minute = now / 60_000;
    if (windows.size() > MAX_TRACKED_CLIENTS) {
      windows.values().removeIf(w -> w.minute != minute);
    }
    String key =
        request.getRemoteAddr() + ' ' + request.getMethod() + ' ' + request.getRequestURI();
    Window window =
        windows.compute(
            key, (k, old) -> old == null || old.minute != minute ? new Window(minute) : old);
    if (window.count.incrementAndGet() > perMinute) {
      response.setStatus(429);
      response.setHeader("Retry-After", String.valueOf(60 - now / 1000 % 60));
      response.setContentType("application/json;charset=UTF-8");
      response.getWriter().write(BODY);
      return;
    }
    chain.doFilter(request, response);
  }

  /** Number of client and endpoint windows currently kept in memory. */
  int trackedClients() {
    return windows.size();
  }

  private static final class Window {
    private final long minute;
    private final AtomicInteger count = new AtomicInteger();

    private Window(long minute) {
      this.minute = minute;
    }
  }
}
