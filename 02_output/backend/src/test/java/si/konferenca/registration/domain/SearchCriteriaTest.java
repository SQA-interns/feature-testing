package si.konferenca.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static si.konferenca.registration.domain.DomainFixtures.configuration;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SearchCriteriaTest {

  private static SearchCriteria.Parsed parse(String text, String type, String option, String page) {
    return SearchCriteria.parse(text, type, option, page, configuration());
  }

  private static SearchCriteria valid(String text, String type, String option, String page) {
    SearchCriteria.Parsed parsed = parse(text, type, option, page);
    assertThat(parsed.errors()).isEmpty();
    return parsed.criteria();
  }

  @Test
  void absentOrEmptyParametersMeanEverythingOnPageOne() {
    SearchCriteria expected = new SearchCriteria("", Optional.empty(), Optional.empty(), 1);

    assertThat(valid(null, null, null, null)).isEqualTo(expected);
    assertThat(valid("", "", "", "")).isEqualTo(expected);
    assertThat(expected.offset()).isZero();
  }

  @Test
  void textIsTrimmedAndLowerCased() {
    assertThat(valid("  ČeŠ Žar  ", null, null, null).text()).isEqualTo("češ žar");
  }

  @Test
  void textLimitAppliesToTheRawText() {
    assertThat(valid("a".repeat(100), null, null, null).text()).hasSize(100);
    SearchCriteria.Parsed parsed = parse(" " + "a".repeat(100), null, null, null);

    assertThat(parsed.criteria()).isNull();
    assertThat(parsed.errors()).containsExactly(new FieldError("text", "At most 100 characters."));
  }

  @Test
  void typeAndConfiguredOptionAreParsed() {
    SearchCriteria criteria = valid(null, "STUDENT", "meal-old", "3");

    assertThat(criteria.type()).contains(RegistrationType.STUDENT);
    assertThat(criteria.option()).contains("meal-old");
    assertThat(criteria.page()).isEqualTo(3);
    assertThat(criteria.offset()).isEqualTo(40);
  }

  @ParameterizedTest
  @ValueSource(strings = {"student", "OTHER", " STUDENT"})
  void unknownTypeIsRejected(String type) {
    assertThat(parse(null, type, null, null).errors())
        .containsExactly(new FieldError("type", "Choose EXTERNAL or STUDENT."));
  }

  @Test
  void unconfiguredOptionIsRejected() {
    assertThat(parse(null, null, "no-such", null).errors())
        .containsExactly(new FieldError("option", "Unknown option."));
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "-1", "abc", "1.5", "99999999999"})
  void invalidPageIsRejected(String page) {
    assertThat(parse(null, null, null, page).errors())
        .containsExactly(new FieldError("page", "The page must be a number from 1."));
  }

  @Test
  void everyInvalidParameterIsReported() {
    assertThat(parse("x".repeat(101), "X", "y", "0").errors())
        .extracting(FieldError::field)
        .containsExactly("text", "type", "option", "page");
  }

  @Test
  void pageSizeIsTwenty() {
    assertThat(SearchCriteria.PAGE_SIZE).isEqualTo(20);
    assertThat(new SearchCriteria("", Optional.empty(), Optional.empty(), 2).offset())
        .isEqualTo(20);
  }
}
