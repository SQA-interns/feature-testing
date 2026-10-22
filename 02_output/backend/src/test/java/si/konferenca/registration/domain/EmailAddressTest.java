package si.konferenca.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmailAddressTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "ana@example.org",
        "a@b.co",
        "first.last+tag@sub.domain.example",
        "čšž@primer.si",
        "a@b.c.d"
      })
  void acceptsValidAddresses(String address) {
    assertThat(EmailAddress.isValid(address)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "ana.example.org",
        "@example.org",
        "ana@",
        "ana@@example.org",
        "ana@ex@ample.org",
        "ana@example",
        "ana@.example.org",
        "ana@example.org.",
        "ana@example..org",
        "ana novak@example.org",
        "ana@exa\tmple.org",
        "ana@example.org\r\nBcc: x@y.z",
        "ana\u0007@example.org",
        "a,b@example.org",
        "a;b@example.org",
        "<a@example.org>",
        "a(b)@example.org",
        "a[b]@example.org",
        "a\\b@example.org",
        "\"a\"@example.org",
        "a:b@example.org"
      })
  void rejectsInvalidAddresses(String address) {
    assertThat(EmailAddress.isValid(address)).isFalse();
  }

  @Test
  void rejectsNull() {
    assertThat(EmailAddress.isValid(null)).isFalse();
  }

  @Test
  void acceptsTheMaximumLengthAndRejectsOneMore() {
    String domain = "@example.org";
    String atMax = "a".repeat(EmailAddress.MAX_LENGTH - domain.length()) + domain;

    assertThat(atMax).hasSize(254);
    assertThat(EmailAddress.isValid(atMax)).isTrue();
    assertThat(EmailAddress.isValid("a" + atMax)).isFalse();
  }
}
