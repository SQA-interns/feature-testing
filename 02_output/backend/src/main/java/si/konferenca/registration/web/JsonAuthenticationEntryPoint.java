package si.konferenca.registration.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/** 401 with a Basic challenge and an Error body that holds no data (AC-008-02). */
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    response.setHeader("WWW-Authenticate", "Basic realm=\"registration-export\"");
    JsonErrors.write(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required.");
  }
}
