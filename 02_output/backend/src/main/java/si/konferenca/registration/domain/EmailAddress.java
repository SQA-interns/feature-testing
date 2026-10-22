package si.konferenca.registration.domain;

/**
 * The email format rule (BR-03): one {@code @} with a non-empty local part and a dotted domain, no
 * whitespace, control or address-list characters, so an address can never add a header or a second
 * recipient (SR-05).
 */
public final class EmailAddress {

  public static final int MAX_LENGTH = 254;
  private static final String FORBIDDEN = ",;<>()[]\\\":";

  private EmailAddress() {}

  public static boolean isValid(String value) {
    if (value == null || value.length() > MAX_LENGTH) {
      return false;
    }
    int at = value.indexOf('@');
    if (at <= 0 || at != value.lastIndexOf('@')) {
      return false;
    }
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (Character.isWhitespace(c) || Character.isISOControl(c) || FORBIDDEN.indexOf(c) >= 0) {
        return false;
      }
    }
    String domain = value.substring(at + 1);
    return domain.indexOf('.') > 0 && !domain.endsWith(".") && !domain.contains("..");
  }
}
