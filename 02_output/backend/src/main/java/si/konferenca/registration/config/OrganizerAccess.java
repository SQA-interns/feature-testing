package si.konferenca.registration.config;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * Organizer endpoints (BR-08, SR-06): the caller must be the organizer, and when HTTPS-only access
 * is on the request must be secure or come from a loopback address. Scheme and client address are
 * those resolved from trusted proxies ({@code server.forward-headers-strategy=native}).
 */
class OrganizerAccess implements AuthorizationManager<RequestAuthorizationContext> {

  private static final String AUTHORITY = "ROLE_" + SecurityConfig.ORGANIZER_ROLE;
  private static final Pattern IP_LITERAL = Pattern.compile("[0-9a-fA-F.:]+");

  private final boolean httpsOnly;

  OrganizerAccess(boolean httpsOnly) {
    this.httpsOnly = httpsOnly;
  }

  @Override
  public AuthorizationResult authorize(
      Supplier<? extends Authentication> authentication, RequestAuthorizationContext context) {
    Authentication caller = authentication.get();
    boolean organizer =
        caller != null
            && caller.getAuthorities().stream().anyMatch(a -> AUTHORITY.equals(a.getAuthority()));
    return new AuthorizationDecision(organizer && transportAllowed(context.getRequest()));
  }

  boolean transportAllowed(HttpServletRequest request) {
    return !httpsOnly || request.isSecure() || isLoopback(request.getRemoteAddr());
  }

  static boolean isLoopback(String address) {
    if (address == null || !IP_LITERAL.matcher(address).matches()) {
      return false;
    }
    try {
      return InetAddress.getByName(address).isLoopbackAddress();
    } catch (UnknownHostException e) {
      return false;
    }
  }
}
