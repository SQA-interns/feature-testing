package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static si.konferenca.registration.acceptance.support.Submissions.validExternal;
import static si.konferenca.registration.acceptance.support.Submissions.validStudent;
import static si.konferenca.registration.acceptance.support.Submissions.with;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import si.konferenca.registration.acceptance.support.HttpApi;
import si.konferenca.registration.acceptance.support.Settings;
import si.konferenca.registration.acceptance.support.StandardAcceptanceTest;
import tools.jackson.databind.JsonNode;

/** US-009 Organizer registration search, through {@code GET /api/registrations}. */
class Us009SearchAcceptanceTest extends StandardAcceptanceTest {

  private static final String SEARCH = "/api/registrations";

  private UUID mojca;
  private UUID peter;
  private UUID ana;

  /** Three registrations: two external, one student, with distinct texts and options. */
  private void registerThree() {
    mojca =
        registerAccepted(
            with(
                validExternal(),
                "firstName",
                "Mojca",
                "lastName",
                "Kranjc",
                "email",
                "mojca.kranjc@alpha.si",
                "organization",
                "Alpha Consulting",
                "options",
                List.of("ws-ai")));
    pause();
    peter =
        registerAccepted(
            with(
                validStudent(),
                "firstName",
                "Peter",
                "lastName",
                "Zupan",
                "email",
                "peter@uni.example.org",
                "studyInstitution",
                "Univerza v Mariboru",
                "options",
                List.of("meal-lunch", "ws-ai")));
    pause();
    ana = registerAccepted(with(validExternal(), "options", List.of("ev-dinner", "meal-lunch")));
  }

  @Test
  @DisplayName("AC-009-01 without text or filters the registrations are listed newest first")
  void ac009_01_listsRegistrationsNewestFirst() {
    registerThree();

    HttpApi.Response response = search(Map.of());

    assertThat(response.status()).as("response %s", response).isEqualTo(200);
    JsonNode body = response.json();
    assertThat(body.path("total").asInt()).isEqualTo(3);
    assertThat(ids(body)).containsExactly(ana, peter, mojca);
    JsonNode first = body.path("items").get(0);
    assertThat(first.path("type").asString()).isEqualTo("EXTERNAL");
    assertThat(first.path("firstName").asString()).isEqualTo("Ana");
    assertThat(first.path("lastName").asString()).isEqualTo("Novak");
    assertThat(first.path("email").asString()).isEqualTo("ana.novak@example.org");
    assertThat(first.path("organization").asString()).isEqualTo("Example d.o.o.");
    assertThat(first.path("options").valueStream().map(JsonNode::asString))
        .containsExactlyInAnyOrder("ev-dinner", "meal-lunch");
    assertThat(OffsetDateTime.parse(first.path("registeredAt").asString())).isNotNull();
    JsonNode student = body.path("items").get(1);
    assertThat(student.path("type").asString()).isEqualTo("STUDENT");
    assertThat(student.path("studyInstitution").asString()).isEqualTo("Univerza v Mariboru");
  }

  @ParameterizedTest(name = "AC-009-02 text \"{0}\" matches {1}")
  @CsvSource({
    "kranjc, mojca",
    "MOJCA, mojca",
    "ALPHA.si, mojca",
    "consulting, mojca",
    "MARIBOR, peter",
    "uni.example, peter",
    "'  zupan  ', peter",
    "nOvAk, ana"
  })
  void ac009_02_textMatchesAnyPartOfTheSearchedFields(String text, String expected) {
    registerThree();
    UUID expectedId =
        switch (expected) {
          case "mojca" -> mojca;
          case "peter" -> peter;
          default -> ana;
        };

    JsonNode body = searchOk(Map.of("text", text));

    assertThat(ids(body)).containsExactly(expectedId);
    assertThat(body.path("total").asInt()).isEqualTo(1);
  }

  @Test
  @DisplayName("AC-009-03 the type filter STUDENT lists only student registrations")
  void ac009_03_typeFilterListsOnlyThatType() {
    registerThree();

    assertThat(ids(searchOk(Map.of("type", "STUDENT")))).containsExactly(peter);
    assertThat(ids(searchOk(Map.of("type", "EXTERNAL")))).containsExactly(ana, mojca);
  }

  @Test
  @DisplayName("AC-009-04 the option filter lists only registrations that selected the option")
  void ac009_04_optionFilterListsOnlyRegistrationsWithTheOption() {
    registerThree();

    assertThat(ids(searchOk(Map.of("option", "meal-lunch")))).containsExactly(ana, peter);
    assertThat(ids(searchOk(Map.of("option", "ev-dinner")))).containsExactly(ana);
    assertThat(ids(searchOk(Map.of("option", "other-tour")))).isEmpty();
  }

  @Test
  @DisplayName("AC-009-05 text, type and option filters together list only full matches")
  void ac009_05_filtersCombine() {
    registerThree();

    assertThat(ids(searchOk(Map.of("text", "example")))).containsExactly(ana, peter);
    assertThat(ids(searchOk(params("text", "example", "type", "EXTERNAL", "option", "meal-lunch"))))
        .containsExactly(ana);
    assertThat(ids(searchOk(params("text", "example", "type", "STUDENT", "option", "ev-dinner"))))
        .isEmpty();
  }

  @Test
  @DisplayName("AC-009-06 when nothing matches the response is an empty list with total 0")
  void ac009_06_noMatchGivesEmptyList() {
    registerThree();

    JsonNode body = searchOk(Map.of("text", "no-such-person"));

    assertThat(body.path("items").isArray()).isTrue();
    assertThat(body.path("items").size()).isZero();
    assertThat(body.path("total").asInt()).isZero();
  }

  @Test
  @DisplayName("AC-009-07 25 matches are paged as the newest 20 and the remaining 5")
  void ac009_07_resultsArePagedByTwenty() {
    Set<UUID> registered = new HashSet<>();
    for (int i = 1; i <= 25; i++) {
      registered.add(
          registerAccepted(
              with(
                  validExternal(),
                  "lastName",
                  "Paged" + i,
                  "email",
                  "paged" + i + "@example.org")));
    }

    JsonNode first = searchOk(Map.of("page", "1"));
    JsonNode second = searchOk(Map.of("page", "2"));

    assertThat(first.path("items").size()).isEqualTo(20);
    assertThat(second.path("items").size()).isEqualTo(5);
    assertThat(first.path("total").asInt()).isEqualTo(25);
    assertThat(second.path("total").asInt()).isEqualTo(25);
    List<UUID> all = new ArrayList<>(ids(first));
    all.addAll(ids(second));
    assertThat(new HashSet<>(all)).isEqualTo(registered);
    List<OffsetDateTime> times = new ArrayList<>(times(first));
    times.addAll(times(second));
    for (int i = 1; i < times.size(); i++) {
      assertThat(times.get(i))
          .as("item %d is not newer than item %d", i, i - 1)
          .isBeforeOrEqualTo(times.get(i - 1));
    }
  }

  @ParameterizedTest(name = "AC-009-08 {0}={1} is rejected")
  @CsvSource({"type, VISITOR", "option, no-such-option", "page, 0", "page, -1"})
  void ac009_08_invalidParameterIsRejected(String parameter, String value) {
    HttpApi.Response response = search(Map.of(parameter, value));

    assertThat(response.status()).as("response %s", response).isEqualTo(400);
    assertThat(response.errorFields()).contains(parameter);
  }

  @Test
  @DisplayName("AC-009-08 a text longer than 100 characters is rejected, 100 are accepted")
  void ac009_08_textLongerThan100IsRejected() {
    HttpApi.Response tooLong = search(Map.of("text", "x".repeat(101)));
    assertThat(tooLong.status()).as("response %s", tooLong).isEqualTo(400);
    assertThat(tooLong.errorFields()).contains("text");

    assertThat(search(Map.of("text", "x".repeat(100))).status()).isEqualTo(200);
    assertThat(search(Map.of("option", "ws-retired")).status())
        .as("an inactive option is still a configured option")
        .isEqualTo(200);
  }

  @Test
  @DisplayName("AC-009-09 the search without or with wrong credentials is refused with 401")
  void ac009_09_searchWithoutValidCredentialsIsRefused() {
    registerThree();
    assertThat(search(Map.of()).status()).as("control: valid credentials").isEqualTo(200);

    List<HttpApi.Response> refused =
        List.of(
            api.get(SEARCH),
            api.get(
                SEARCH,
                "Authorization",
                HttpApi.basic(Settings.ORGANIZER_USERNAME, "wrong-pass-123")),
            api.get(
                SEARCH, "Authorization", HttpApi.basic("intruder", Settings.ORGANIZER_PASSWORD)));
    for (HttpApi.Response response : refused) {
      assertThat(response.status()).isEqualTo(401);
      assertThat(response.text()).doesNotContain("Novak", "Kranjc", "Zupan", "example.org");
    }
  }

  @Test
  @DisplayName(
      "AC-009-10 with HTTPS-only on, the search over plain HTTP from a remote client is refused")
  void ac009_10_plainHttpFromRemoteClientIsRefused() {
    registerThree();

    HttpApi.Response plain = api.getAsOrganizer(SEARCH, "X-Forwarded-For", "198.51.100.23");
    HttpApi.Response secure =
        api.getAsOrganizer(
            SEARCH, "X-Forwarded-For", "198.51.100.23", "X-Forwarded-Proto", "https");

    assertThat(plain.status()).as("plain HTTP from a remote client").isBetween(400, 499);
    assertThat(plain.text()).doesNotContain("Novak", "Kranjc", "Zupan", "example.org");
    assertThat(secure.status()).as("control: the same client over HTTPS").isEqualTo(200);
  }

  @Test
  @DisplayName("AC-009-11 a Slovenian last name is found and returned unchanged")
  void ac009_11_slovenianLastNameIsFoundUnchanged() {
    registerThree();
    String lastName = "Žagar-Čuk Šuštar";
    UUID id = registerAccepted(with(validExternal(), "lastName", lastName));

    JsonNode body = searchOk(Map.of("text", lastName));

    assertThat(ids(body)).containsExactly(id);
    assertThat(body.path("items").get(0).path("lastName").asString()).isEqualTo(lastName);
  }

  private HttpApi.Response search(Map<String, String> parameters) {
    return api.getAsOrganizer(SEARCH + HttpApi.query(parameters));
  }

  private JsonNode searchOk(Map<String, String> parameters) {
    HttpApi.Response response = search(parameters);
    assertThat(response.status()).as("search %s: %s", parameters, response).isEqualTo(200);
    return response.json();
  }

  private static Map<String, String> params(String... pairs) {
    Map<String, String> map = new LinkedHashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      map.put(pairs[i], pairs[i + 1]);
    }
    return map;
  }

  private static List<UUID> ids(JsonNode body) {
    return body.path("items")
        .valueStream()
        .map(item -> UUID.fromString(item.path("id").asString()))
        .toList();
  }

  private static List<OffsetDateTime> times(JsonNode body) {
    return body.path("items")
        .valueStream()
        .map(item -> OffsetDateTime.parse(item.path("registeredAt").asString()))
        .toList();
  }

  /** Keeps registration times distinct, so "newest first" has one correct order. */
  private static void pause() {
    try {
      Thread.sleep(50);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
