package si.konferenca.registration.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * 401 with an Error body that holds no data (AC-008-02, AC-009-03). The export carries a Basic
 * challenge; the organizer search does not, so a browser opens no login dialog over the organizer
 * page (D-26).
 */
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    if (!HttpsOnlyFilter.SEARCH_PATH.equals(request.getRequestURI())) {
      response.setHeader("WWW-Authenticate", "Basic realm=\"registration-export\"");
    }
    JsonErrors.write(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required.");
  }
}
