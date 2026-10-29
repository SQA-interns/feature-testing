package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import si.konferenca.registration.acceptance.support.OrganizerSearchTestBase;
import si.konferenca.registration.acceptance.support.Submissions;
import tools.jackson.databind.JsonNode;

/** US-009 search text (BR-11, OQ-07), through GET /api/organizer/registrations (CR-001). */
class OrganizerSearchTextAcceptanceTest extends OrganizerSearchTestBase {

  @DynamicPropertySource
  static void configure(DynamicPropertyRegistry registry) {
    register(registry, Map.of("RATE_LIMIT_ORGANIZER_SEARCH_PER_MINUTE", "10000"));
  }

  private static final Map<String, String> ALL = Map.of();

  // ---- Search text (AC-009-08..16) ----

  static Stream<Arguments> searchedFields() {
    return Stream.of(
        Arguments.of("first name", fieldWith("firstName", "Xylophonia")),
        Arguments.of("last name", fieldWith("lastName", "Xylowski")),
        Arguments.of("email", fieldWith("email", "xylo.fan@example.org")),
        Arguments.of("organization", fieldWith("organization", "Xylo Labs")),
        Arguments.of(
            "study institution",
            (Supplier<Map<String, Object>>)
                () -> {
                  Map<String, Object> body = Submissions.validStudent();
                  body.put("studyInstitution", "Xylo University");
                  return body;
                }));
  }

  private static Supplier<Map<String, Object>> fieldWith(String field, String value) {
    return () -> {
      Map<String, Object> body = Submissions.validExternal();
      body.put(field, value);
      return body;
    };
  }

  @ParameterizedTest(name = "AC-009-08 search text in the {0} lists the registration")
  @MethodSource("searchedFields")
  void ac_009_08_searchTextInAFieldMatches(String field, Supplier<Map<String, Object>> target) {
    String targetId = registered(target.get());
    registered(Submissions.validExternal());
    Map<String, Object> programmeOnly = Submissions.validStudent();
    programmeOnly.put("studyProgramme", "Xylology");
    registered(programmeOnly);

    JsonNode result = search(params("search", "ylo"));

    assertThat(ids(result)).containsExactly(targetId);
  }

  @ParameterizedTest(name = "AC-009-09 search \"{0}\" ignores case")
  @ValueSource(strings = {"NOVAK", "novak"})
  void ac_009_09_caseIsIgnored(String text) {
    String novak = registered(externalNamed("Novak", 1));
    registered(externalNamed("Kranjc", 2));

    assertThat(ids(search(params("search", text)))).containsExactly(novak);
  }

  @Test
  @DisplayName("AC-009-10 \"kos\" lists Koš, Kos and Kós")
  void ac_009_10_letterWithoutAccentMatchesAccentedLetters() {
    registered(externalNamed("Koš", 1));
    registered(externalNamed("Kos", 2));
    registered(externalNamed("Kós", 3));
    registered(externalNamed("Kranjc", 4));

    assertThat(lastNames(search(params("search", "kos"))))
        .containsExactlyInAnyOrder("Koš", "Kos", "Kós");
  }

  @ParameterizedTest(name = "AC-009-11 \"{0}\" lists only Koš")
  @ValueSource(strings = {"koš", "KOŠ"})
  void ac_009_11_accentedLetterMatchesOnlyItself(String text) {
    registered(externalNamed("Koš", 1));
    registered(externalNamed("Kos", 2));

    assertThat(lastNames(search(params("search", text)))).containsExactly("Koš");
  }

  @Test
  @DisplayName("AC-009-12 \"č\" lists only Čač, not Ćać or Cac")
  void ac_009_12_accentedLetterDoesNotMatchOtherAccents() {
    registered(externalNamed("Čač", 1));
    registered(externalNamed("Ćać", 2));
    registered(externalNamed("Cac", 3));

    assertThat(lastNames(search(params("search", "č")))).containsExactly("Čač");
  }

  @Test
  @DisplayName("AC-009-13 \"duric\" lists Đurić and Durić")
  void ac_009_13_dMatchesDWithStroke() {
    registered(externalNamed("Đurić", 1));
    registered(externalNamed("Durić", 2));
    registered(externalNamed("Kranjc", 3));

    assertThat(lastNames(search(params("search", "duric"))))
        .containsExactlyInAnyOrder("Đurić", "Durić");
  }

  @Test
  @DisplayName("AC-009-14 leading and trailing spaces do not change the result")
  void ac_009_14_surroundingSpacesAreIgnored() {
    registered(externalNamed("Novak", 1));
    registered(externalNamed("Kranjc", 2));

    List<String> plain = ids(search(params("search", "novak")));
    List<String> padded = ids(search(params("search", "  novak \t ")));

    assertThat(plain).hasSize(1);
    assertThat(padded).isEqualTo(plain);
  }

  @ParameterizedTest(name = "AC-009-15 search text \"{0}\" lists every registration")
  @ValueSource(strings = {"", "   ", " \t "})
  void ac_009_15_emptySearchTextListsEverything(String text) {
    String external = registered(Submissions.validExternal());
    String student = registered(Submissions.validStudent());

    assertThat(ids(search(params("search", text)))).containsExactlyInAnyOrder(external, student);
    assertThat(ids(search(ALL))).containsExactlyInAnyOrder(external, student);
  }

  @Test
  @DisplayName("AC-009-16 a search text over 100 characters is rejected with a search field error")
  void ac_009_16_searchTextOver100CharactersIsRejected() {
    String id = registered(Submissions.validExternal());

    HttpResponse<String> response = searchAsOrganizer(params("search", "a".repeat(101)));

    assertThat(response.statusCode()).isEqualTo(400);
    assertThat(errorFields(response)).contains("search");
    assertNoRegistrationData(response.body(), id);

    assertThat(searchAsOrganizer(params("search", "a".repeat(100))).statusCode())
        .as("100 characters are allowed")
        .isEqualTo(200);
    assertThat(searchAsOrganizer(params("search", "  " + "a".repeat(100) + "  ")).statusCode())
        .as("the limit applies after leading and trailing spaces are removed")
        .isEqualTo(200);
  }
}
