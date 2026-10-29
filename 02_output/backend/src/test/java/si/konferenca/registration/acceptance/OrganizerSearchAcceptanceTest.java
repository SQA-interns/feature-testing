package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import si.konferenca.registration.acceptance.support.OrganizerSearchTestBase;
import si.konferenca.registration.acceptance.support.Submissions;
import tools.jackson.databind.JsonNode;

/**
 * US-009 Organizer registration search, through GET /api/organizer/registrations (CR-001): access,
 * result list, filters and counts. The search text is in {@link OrganizerSearchTextAcceptanceTest}.
 */
class OrganizerSearchAcceptanceTest extends OrganizerSearchTestBase {

  @DynamicPropertySource
  static void configure(DynamicPropertyRegistry registry) {
    register(registry, Map.of("RATE_LIMIT_ORGANIZER_SEARCH_PER_MINUTE", "10000"));
  }

  private static final Map<String, String> ALL = Map.of();

  // ---- Access (AC-009-03, AC-009-05) ----

  static Stream<Arguments> badCredentials() {
    return Stream.of(
        Arguments.of("no credentials", null),
        Arguments.of("wrong password", basicAuth(ORGANIZER_USERNAME, "wrong-password-123456")),
        Arguments.of("wrong username", basicAuth("someone", ORGANIZER_PASSWORD)));
  }

  @ParameterizedTest(name = "AC-009-03 search refused with {0}")
  @MethodSource("badCredentials")
  void ac_009_03_searchWithoutValidCredentialsIsRefused(String variant, String authorization) {
    String id = registered(Submissions.validExternal());
    var request = searchRequest(ALL);
    if (authorization != null) {
      request.header("Authorization", authorization);
    }

    HttpResponse<String> response = send(request.GET());

    assertThat(response.statusCode()).isEqualTo(401);
    assertNoRegistrationData(response.body(), id);
  }

  @Test
  @DisplayName(
      "AC-009-05 valid credentials over plain HTTP from a non-localhost client are refused")
  void ac_009_05_plainHttpFromRemoteClientIsRefused() {
    String id = registered(Submissions.validExternal());
    String credentials = basicAuth(ORGANIZER_USERNAME, ORGANIZER_PASSWORD);

    HttpResponse<String> overHttps =
        send(
            searchRequest(ALL)
                .header("Authorization", credentials)
                .header("X-Forwarded-For", "203.0.113.10")
                .header("X-Forwarded-Proto", "https")
                .GET());
    assertThat(overHttps.statusCode())
        .as("control: the same remote client over HTTPS (via the proxy) is served")
        .isEqualTo(200);

    HttpResponse<String> overHttp =
        send(
            searchRequest(ALL)
                .header("Authorization", credentials)
                .header("X-Forwarded-For", "203.0.113.10")
                .header("X-Forwarded-Proto", "http")
                .GET());

    assertThat(overHttp.statusCode()).as("refused").isBetween(400, 499);
    assertNoRegistrationData(overHttp.body(), id);
  }

  // ---- Result list (AC-009-06, AC-009-07, AC-009-23) ----

  @Test
  @DisplayName("AC-009-06 every registration is listed, newest first, with the number of matches")
  void ac_009_06_listsEveryRegistrationNewestFirstWithCount() {
    List<String> created = new ArrayList<>();
    created.add(registered(externalNamed("First", 1)));
    created.add(registered(Submissions.validStudent()));
    created.add(registered(externalNamed("Third", 3)));

    JsonNode result = search(ALL);

    assertThat(ids(result)).containsExactly(created.get(2), created.get(1), created.get(0));
    assertThat(result.path("count").asInt()).isEqualTo(3);
  }

  private static List<String> keys(JsonNode node) {
    return new ArrayList<>(node.propertyNames());
  }

  private static JsonNode row(JsonNode result, String id) {
    for (JsonNode r : result.path("registrations")) {
      if (id.equals(r.path("id").asString())) {
        return r;
      }
    }
    throw new AssertionError("registration " + id + " not listed");
  }

  private static void assertConsentsGiven(JsonNode row) {
    JsonNode consents = row.path("consents");
    assertThat(consents.size()).isEqualTo(1);
    JsonNode consent = consents.get(0);
    assertThat(keys(consent)).containsExactlyInAnyOrder("id", "givenAt");
    assertThat(consent.path("id").asString()).isEqualTo("privacy");
    assertThatCode(() -> Instant.parse(consent.path("givenAt").asString()))
        .as("consent time is a UTC timestamp")
        .doesNotThrowAnyException();
  }

  private static List<String> options(JsonNode row) {
    List<String> options = new ArrayList<>();
    for (JsonNode o : row.path("options")) {
      assertThat(keys(o)).containsExactlyInAnyOrder("id", "name", "category");
      options.add(
          o.path("id").asString()
              + "|"
              + o.path("name").asString()
              + "|"
              + o.path("category").asString());
    }
    return options;
  }

  @Test
  @DisplayName(
      "AC-009-07 each registration carries its type, fields, options, consents and time, and"
          + " nothing else")
  void ac_009_07_rowHoldsOnlyTheExportData() {
    String externalId = registered(Submissions.validExternal());
    String studentId = registered(Submissions.validStudent());

    JsonNode result = search(ALL);

    assertThat(keys(result)).containsExactlyInAnyOrder("count", "registrations", "optionCounts");

    JsonNode external = row(result, externalId);
    assertThat(keys(external))
        .containsExactlyInAnyOrder(
            "id",
            "type",
            "registeredAt",
            "firstName",
            "lastName",
            "email",
            "organization",
            "options",
            "consents");
    assertThat(external.path("type").asString()).isEqualTo("EXTERNAL");
    assertThat(external.path("firstName").asString()).isEqualTo("Ana");
    assertThat(external.path("lastName").asString()).isEqualTo("Novak");
    assertThat(external.path("email").asString()).isEqualTo("ana.novak@example.org");
    assertThat(external.path("organization").asString()).isEqualTo("Example Institute");
    assertThat(options(external))
        .containsExactlyInAnyOrder(
            "ws-testing|Workshop: Testing in practice|WORKSHOP",
            "ev-dinner|Conference dinner|EVENT");
    assertConsentsGiven(external);
    assertThatCode(() -> Instant.parse(external.path("registeredAt").asString()))
        .doesNotThrowAnyException();

    JsonNode student = row(result, studentId);
    assertThat(keys(student))
        .containsExactlyInAnyOrder(
            "id",
            "type",
            "registeredAt",
            "firstName",
            "lastName",
            "email",
            "studyInstitution",
            "studyProgramme",
            "studentId",
            "options",
            "consents");
    assertThat(student.path("type").asString()).isEqualTo("STUDENT");
    assertThat(student.path("firstName").asString()).isEqualTo("Luka");
    assertThat(student.path("lastName").asString()).isEqualTo("Kranjc");
    assertThat(student.path("email").asString()).isEqualTo("luka.kranjc@student.example.org");
    assertThat(student.path("studyInstitution").asString()).isEqualTo("University of Ljubljana");
    assertThat(student.path("studyProgramme").asString()).isEqualTo("Computer Science");
    assertThat(student.path("studentId").asString()).isEqualTo("63200001");
    assertThat(options(student))
        .containsExactlyInAnyOrder(
            "ws-testing|Workshop: Testing in practice|WORKSHOP",
            "meal-lunch|Lunch (vegetarian)|MEAL");
    assertConsentsGiven(student);
    assertThatCode(() -> Instant.parse(student.path("registeredAt").asString()))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("AC-009-23 no match gives an empty list and the number of matches 0")
  void ac_009_23_noMatchGivesZero() {
    registered(Submissions.validExternal());

    JsonNode result = search(params("search", "nobody-has-this-text"));

    assertThat(result.path("count").asInt()).isZero();
    assertThat(result.path("registrations").size()).isZero();
  }

  // ---- Filters (AC-009-18..20, AC-009-22) ----

  @Test
  @DisplayName("AC-009-18 the type filter lists only registrations of that type")
  void ac_009_18_typeFilter() {
    String external = registered(Submissions.validExternal());
    String student = registered(Submissions.validStudent());

    assertThat(ids(search(params("type", "STUDENT")))).containsExactly(student);
    assertThat(ids(search(params("type", "EXTERNAL")))).containsExactly(external);
  }

  @Test
  @DisplayName("AC-009-19 a category filter lists only registrations that selected the option")
  void ac_009_19_categoryFilter() {
    String testingAndDinner = registered(Submissions.validExternal());
    String testingAndLunch = registered(Submissions.validStudent());
    Map<String, Object> industryOnly = externalNamed("Industry", 3);
    industryOnly.put("optionIds", new ArrayList<>(List.of("ws-industry")));
    String industry = registered(industryOnly);
    Map<String, Object> tourOnly = externalNamed("Tour", 4);
    tourOnly.put("optionIds", new ArrayList<>(List.of("other-tour")));
    String tour = registered(tourOnly);

    assertThat(ids(search(params("workshop", "ws-industry")))).containsExactly(industry);
    assertThat(ids(search(params("workshop", "ws-testing"))))
        .containsExactlyInAnyOrder(testingAndDinner, testingAndLunch);
    assertThat(ids(search(params("event", "ev-dinner")))).containsExactly(testingAndDinner);
    assertThat(ids(search(params("meal", "meal-lunch")))).containsExactly(testingAndLunch);
    assertThat(ids(search(params("other", "other-tour")))).containsExactly(tour);
  }

  @Test
  @DisplayName("AC-009-20 search text, type filter and category filter combine")
  void ac_009_20_allConditionsMustHold() {
    Map<String, Object> externalWithLunch = externalNamed("Novak", 1);
    externalWithLunch.put("optionIds", new ArrayList<>(List.of("meal-lunch")));
    registered(externalWithLunch);
    Map<String, Object> studentNovakLunch = Submissions.validStudent();
    studentNovakLunch.put("lastName", "Novak");
    String match = registered(studentNovakLunch);
    Map<String, Object> studentNovakNoLunch = Submissions.validStudent();
    studentNovakNoLunch.put("lastName", "Novak");
    studentNovakNoLunch.put("optionIds", new ArrayList<>(List.of("ws-testing")));
    registered(studentNovakNoLunch);
    registered(Submissions.validStudent());

    JsonNode result = search(params("search", "novak", "type", "STUDENT", "meal", "meal-lunch"));

    assertThat(ids(result)).containsExactly(match);
  }

  static Stream<Arguments> invalidFilters() {
    return Stream.of(
        Arguments.of("unknown registration type", "type", "VISITOR"),
        Arguments.of("unknown option id", "workshop", "no-such-option"),
        Arguments.of("meal option in the workshop filter", "workshop", "meal-lunch"),
        Arguments.of("workshop option in the meal filter", "meal", "ws-testing"));
  }

  @ParameterizedTest(name = "AC-009-22 rejected: {0}")
  @MethodSource("invalidFilters")
  void ac_009_22_invalidFilterIsRejected(String variant, String filter, String value) {
    String id = registered(Submissions.validExternal());

    HttpResponse<String> response = searchAsOrganizer(params(filter, value));

    assertThat(response.statusCode()).isEqualTo(400);
    assertThat(errorFields(response)).contains(filter);
    assertNoRegistrationData(response.body(), id);
  }

  // ---- Option counts (AC-009-24, AC-009-25) ----

  @Test
  @DisplayName(
      "AC-009-24 every configured option, active and inactive, is counted by category, zero"
          + " included")
  void ac_009_24_everyOptionIsCountedByCategory() {
    registered(Submissions.validExternal());
    registered(Submissions.validStudent());

    JsonNode result = search(ALL);

    List<String> categories = new ArrayList<>();
    for (JsonNode c : result.path("optionCounts")) {
      categories.add(c.path("category").asString());
    }
    assertThat(categories).containsExactly("WORKSHOP", "EVENT", "MEAL", "OTHER");
    assertThat(counts(result, "WORKSHOP"))
        .containsExactly(
            Map.entry("ws-testing", 2), Map.entry("ws-industry", 0), Map.entry("ws-legacy", 0));
    assertThat(counts(result, "EVENT")).containsExactly(Map.entry("ev-dinner", 1));
    assertThat(counts(result, "MEAL")).containsExactly(Map.entry("meal-lunch", 1));
    assertThat(counts(result, "OTHER")).containsExactly(Map.entry("other-tour", 0));

    JsonNode none = search(params("search", "nobody-has-this-text"));
    assertThat(counts(none, "WORKSHOP"))
        .containsExactly(
            Map.entry("ws-testing", 0), Map.entry("ws-industry", 0), Map.entry("ws-legacy", 0));
    assertThat(counts(none, "OTHER")).containsExactly(Map.entry("other-tour", 0));
  }

  @Test
  @DisplayName("AC-009-25 with the student filter the meal counts include only students")
  void ac_009_25_countsFollowTheTypeFilter() {
    Map<String, Object> externalWithLunch = Submissions.validExternal();
    externalWithLunch.put("optionIds", new ArrayList<>(List.of("meal-lunch")));
    registered(externalWithLunch);
    registered(Submissions.validStudent());

    assertThat(counts(search(ALL), "MEAL")).containsExactly(Map.entry("meal-lunch", 2));
    JsonNode students = search(params("type", "STUDENT"));
    assertThat(counts(students, "MEAL")).containsExactly(Map.entry("meal-lunch", 1));
    assertThat(students.path("count").asInt()).isEqualTo(1);
  }
}
