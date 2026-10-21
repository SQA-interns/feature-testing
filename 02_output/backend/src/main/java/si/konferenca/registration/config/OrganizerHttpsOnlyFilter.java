package si.konferenca.registration.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * With HTTPS-only organizer access on, refuses the authorized export over plain HTTP from a client
 * that is not on the loopback interface (SR-06, AC-008-04). Runs after authorization, so missing or
 * wrong credentials are still answered with 401.
 */
public class OrganizerHttpsOnlyFilter extends OncePerRequestFilter {

  static final String EXPORT_PATH = "/api/export";
  static final String BODY = "{\"message\":\"Organizer access requires HTTPS.\"}";

  private final boolean httpsOnly;

  public OrganizerHttpsOnlyFilter(boolean httpsOnly) {
    this.httpsOnly = httpsOnly;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !httpsOnly
        || !EXPORT_PATH.equals(
            request.getRequestURI().substring(request.getContextPath().length()));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (request.isSecure() || isLoopback(request.getRemoteAddr())) {
      chain.doFilter(request, response);
      return;
    }
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType("application/json;charset=UTF-8");
    response.getOutputStream().write(BODY.getBytes(StandardCharsets.UTF_8));
  }

  static boolean isLoopback(String address) {
    if (address == null || address.isBlank()) {
      return false;
    }
    try {
      // the servlet container reports an IP literal, so no name lookup takes place
      return InetAddress.getByName(address).isLoopbackAddress();
    } catch (UnknownHostException e) {
      return false;
    }
  }
}
