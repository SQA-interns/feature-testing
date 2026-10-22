package si.konferenca.registration.adapter.web;

import jakarta.servlet.http.HttpServletRequest;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

/**
 * Who sent a request and how ({@code docs/02_specification.md} 8.2). Forwarded headers count only
 * when the direct peer is a trusted proxy (loopback or private address); the client is then the
 * last {@code X-Forwarded-For} entry, the one that proxy added.
 */
public final class ClientAddress {

  private static final Pattern IP_LITERAL = Pattern.compile("[0-9A-Fa-f:.]{2,45}");

  private ClientAddress() {}

  public static String of(HttpServletRequest request) {
    String peer = request.getRemoteAddr();
    if (isTrustedProxy(peer)) {
      String forwarded = request.getHeader("X-Forwarded-For");
      if (forwarded != null && !forwarded.isBlank()) {
        String[] hops = forwarded.split(",");
        String last = hops[hops.length - 1].strip();
        if (!last.isEmpty()) {
          return last;
        }
      }
    }
    return peer;
  }

  /** TLS to this process, or TLS to a trusted proxy that says so. */
  public static boolean isSecure(HttpServletRequest request) {
    if (request.isSecure()) {
      return true;
    }
    String proto = request.getHeader("X-Forwarded-Proto");
    return proto != null
        && isTrustedProxy(request.getRemoteAddr())
        && "https".equalsIgnoreCase(proto.split(",")[0].strip());
  }

  /** The client is this machine (loopback). */
  public static boolean isLocal(HttpServletRequest request) {
    InetAddress address = parse(of(request));
    return address != null && address.isLoopbackAddress();
  }

  static boolean isTrustedProxy(String peer) {
    InetAddress address = parse(peer);
    return address != null
        && (address.isLoopbackAddress()
            || address.isSiteLocalAddress()
            || (address instanceof Inet6Address && (address.getAddress()[0] & 0xfe) == 0xfc));
  }

  /** Parses an IP literal only; never a name lookup. */
  private static InetAddress parse(String literal) {
    if (literal == null || !IP_LITERAL.matcher(literal).matches()) {
      return null;
    }
    try {
      return InetAddress.getByName(literal);
    } catch (UnknownHostException e) {
      return null;
    }
  }
}
