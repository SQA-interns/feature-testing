package si.konferenca.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmailAddressTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "a@b.si",
        "ana.novak@example.org",
        "ana+tag@sub.example.co.uk",
        "o'neil@example.org",
        "a-b@my-host.example.org"
      })
  void acceptsPlainAddresses(String value) {
    assertThat(EmailAddress.isValid(value)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "ana",
        "@example.org",
        "ana@",
        "ana@@example.org",
        "ana@x@example.org",
        "ana.@example.org",
        ".ana@example.org",
        "an..a@example.org",
        "ana novak@example.org",
        "ana@example",
        "ana@example.o",
        "ana@-host.example.org",
        "ana@host-.example.org",
        "ana@example..org",
        "ana@example.org.",
        "Ana <ana@example.org>",
        "ana@example.org,eve@example.org"
      })
  void rejectsOtherForms(String value) {
    assertThat(EmailAddress.isValid(value)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 63, 64})
  void acceptsLocalPartUpTo64Characters(int length) {
    assertThat(EmailAddress.isValid("a".repeat(length) + "@example.org")).isTrue();
  }

  @ParameterizedTest
  @ValueSource(ints = {65, 100})
  void rejectsLocalPartLongerThan64Characters(int length) {
    assertThat(EmailAddress.isValid("a".repeat(length) + "@example.org")).isFalse();
  }
}
