package si.konferenca.registration.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * SR-06: the export is refused over plain HTTP unless the client is on localhost; the request is
 * answered 403 before any credential is read, and never redirected (D-11).
 */
public class HttpsOnlyFilter extends OncePerRequestFilter {

  static final String EXPORT_PATH = "/api/registrations/export";

  private final boolean httpsOnly;

  public HttpsOnlyFilter(boolean httpsOnly) {
    this.httpsOnly = httpsOnly;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !httpsOnly || !EXPORT_PATH.equals(request.getRequestURI());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (request.isSecure() || isLoopback(request.getRemoteAddr())) {
      chain.doFilter(request, response);
    } else {
      JsonErrors.write(
          response, HttpServletResponse.SC_FORBIDDEN, "Organizer access requires HTTPS.");
    }
  }

  static boolean isLoopback(String address) {
    if (address == null || !address.matches("[0-9a-fA-F:.]+")) {
      return false;
    }
    try {
      return InetAddress.getByName(address).isLoopbackAddress();
    } catch (UnknownHostException e) {
      return false;
    }
  }
}
