package si.konferenca.registration.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Refuses organizer requests over plain HTTP unless they come from localhost (SR-06, D-10). The
 * check runs before authentication, so a refused request never tells whether its credentials were
 * valid. Scheme and address already reflect X-Forwarded-* from trusted proxies only.
 */
public class HttpsOnlyFilter extends OncePerRequestFilter {

  private final boolean httpsOnly;

  public HttpsOnlyFilter(boolean httpsOnly) {
    this.httpsOnly = httpsOnly;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (httpsOnly
        && isOrganizerRequest(request)
        && !request.isSecure()
        && !isLoopback(request.getRemoteAddr())) {
      JsonAuthenticationEntryPoint.writeJson(
          response, HttpServletResponse.SC_FORBIDDEN, "Organizer access requires HTTPS.");
      return;
    }
    chain.doFilter(request, response);
  }

  static boolean isOrganizerRequest(HttpServletRequest request) {
    String path = request.getRequestURI().substring(request.getContextPath().length());
    return "GET".equals(request.getMethod())
        && ("/api/export".equals(path) || "/api/registrations".equals(path));
  }

  static boolean isLoopback(String address) {
    return address != null
        && (address.startsWith("127.")
            || "::1".equals(address)
            || "0:0:0:0:0:0:0:1".equals(address));
  }
}
