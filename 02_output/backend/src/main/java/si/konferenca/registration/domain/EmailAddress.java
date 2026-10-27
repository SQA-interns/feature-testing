package si.konferenca.registration.domain;

import java.util.regex.Pattern;

/** The email format of specification S4.2 (BR-03): one plain address, no display name. */
public final class EmailAddress {

  private static final int MAX_LOCAL_PART = 64;
  private static final Pattern LOCAL_PART =
      Pattern.compile("[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*");
  private static final Pattern DOMAIN =
      Pattern.compile("(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\\.)+[A-Za-z]{2,}");

  private EmailAddress() {}

  /** Whether the (already trimmed) value is a valid address. */
  public static boolean isValid(String value) {
    int at = value.indexOf('@');
    if (at <= 0 || at != value.lastIndexOf('@')) {
      return false;
    }
    String local = value.substring(0, at);
    String domain = value.substring(at + 1);
    return local.length() <= MAX_LOCAL_PART
        && LOCAL_PART.matcher(local).matches()
        && DOMAIN.matcher(domain).matches();
  }
}
