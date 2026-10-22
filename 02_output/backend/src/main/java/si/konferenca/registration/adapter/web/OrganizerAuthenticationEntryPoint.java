package si.konferenca.registration.adapter.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Answers 401 without registration data (SB-02). Browser scripts send {@code X-Requested-With:
 * XMLHttpRequest} and get no {@code WWW-Authenticate} challenge, so no browser login dialog appears
 * on the organizer page.
 */
public class OrganizerAuthenticationEntryPoint implements AuthenticationEntryPoint {

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    if (!"XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
      response.setHeader("WWW-Authenticate", "Basic realm=\"organizer\"");
    }
    ErrorResponses.write(
        response,
        HttpServletResponse.SC_UNAUTHORIZED,
        "credentials",
        "Valid organizer credentials are required.");
  }
}
