package si.konferenca.registration.adapter.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * With HTTPS-only access on, refuses the organizer endpoints over plain HTTP unless the client is
 * local (SR-06, AC-008-04, AC-009-10), before any credential is checked (D-11).
 */
public class HttpsOnlyFilter extends OncePerRequestFilter {

  private final boolean httpsOnly;

  public HttpsOnlyFilter(boolean httpsOnly) {
    this.httpsOnly = httpsOnly;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return !httpsOnly
        || !(path.equals("/api/export") || path.equals("/api/registrations"))
        || !"GET".equals(request.getMethod());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (!ClientAddress.isSecure(request) && !ClientAddress.isLocal(request)) {
      ErrorResponses.write(
          response,
          HttpServletResponse.SC_FORBIDDEN,
          "protocol",
          "Organizer access requires HTTPS.");
      return;
    }
    chain.doFilter(request, response);
  }
}
