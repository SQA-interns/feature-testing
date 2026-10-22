package si.konferenca.registration.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Fixed one-minute window per client address and endpoint for every /api request (SR-03, SB-06);
 * over the limit the answer is 429 with Retry-After.
 */
public class RateLimitFilter extends OncePerRequestFilter {

  static final long WINDOW_MILLIS = 60_000;
  private static final int MAX_TRACKED = 10_000;

  private final int limit;
  private final Clock clock;
  private final Map<String, Window> windows = new ConcurrentHashMap<>();

  private record Window(long start, int count) {}

  public RateLimitFilter(int limit, Clock clock) {
    this.limit = limit;
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
    long windowStart = now - now % WINDOW_MILLIS;
    String key =
        request.getRemoteAddr() + ' ' + request.getMethod() + ' ' + request.getRequestURI();
    if (windows.size() > MAX_TRACKED) {
      windows.values().removeIf(w -> w.start() < windowStart);
    }
    Window window =
        windows.merge(
            key,
            new Window(windowStart, 1),
            (old, fresh) ->
                old.start() == windowStart ? new Window(old.start(), old.count() + 1) : fresh);
    if (window.count() > limit) {
      long retryAfter = (windowStart + WINDOW_MILLIS - now + 999) / 1000;
      response.setHeader("Retry-After", String.valueOf(retryAfter));
      JsonAuthenticationEntryPoint.writeJson(response, 429, "Too many requests.");
      return;
    }
    chain.doFilter(request, response);
  }
}
