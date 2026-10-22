package si.konferenca.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SearchRequestValidatorTest {

  private final SearchRequestValidator validator =
      new SearchRequestValidator(Catalogues.standard());

  private static List<String> fields(SearchRequestValidator.Result result) {
    return result.errors().stream().map(FieldError::field).toList();
  }

  @Test
  void absentParametersGiveTheDefaults() {
    SearchCriteria criteria = validator.validate(null, null, null, null).criteria();

    assertThat(criteria).isEqualTo(new SearchCriteria("", null, null, 1));
  }

  @Test
  void emptyParametersCountAsAbsent() {
    SearchCriteria criteria = validator.validate("", "", "", "").criteria();

    assertThat(criteria).isEqualTo(new SearchCriteria("", null, null, 1));
  }

  @Test
  void validParametersAreParsedAndTextTrimmed() {
    SearchRequestValidator.Result result = validator.validate("  novak ", "STUDENT", "old", " 3 ");

    assertThat(result.errors()).isEmpty();
    assertThat(result.criteria())
        .isEqualTo(new SearchCriteria("novak", RegistrationType.STUDENT, "old", 3));
  }

  @Test
  void textOfOneHundredCharactersIsAcceptedButNotMore() {
    assertThat(validator.validate("x".repeat(100), null, null, null).errors()).isEmpty();
    SearchRequestValidator.Result tooLong = validator.validate("x".repeat(101), null, null, null);

    assertThat(fields(tooLong)).containsExactly("text");
    assertThat(tooLong.criteria()).isNull();
    assertThat(tooLong.errors().getFirst().message()).contains("100");
  }

  @Test
  void unknownTypeAndOptionAreRejected() {
    SearchRequestValidator.Result result = validator.validate(null, "student", "nope", null);

    assertThat(fields(result)).containsExactly("type", "option");
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "-1", "two", "1.5", "99999999999"})
  void invalidPageIsRejected(String page) {
    SearchRequestValidator.Result result = validator.validate(null, null, null, page);

    assertThat(fields(result)).containsExactly("page");
    assertThat(result.errors().getFirst().message()).contains("1 or more");
  }

  @Test
  void pageOneIsTheSmallestValidPage() {
    assertThat(validator.validate(null, null, null, "1").criteria().page()).isEqualTo(1);
  }

  @Test
  void allInvalidParametersAreReportedTogether() {
    assertThat(fields(validator.validate("x".repeat(101), "X", "nope", "0")))
        .containsExactly("text", "type", "option", "page");
  }

  @Test
  void pagesHoldTwentyAndKeepTheirItems() {
    SearchPage page = new SearchPage(25, 2, List.of());

    assertThat(SearchCriteria.PAGE_SIZE).isEqualTo(20);
    assertThat(page.total()).isEqualTo(25);
    assertThat(page.items()).isEmpty();
  }
}
