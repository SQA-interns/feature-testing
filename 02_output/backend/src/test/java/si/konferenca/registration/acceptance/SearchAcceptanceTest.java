package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;

/** US-009 organizer registration search: AC-009-01 … AC-009-11 (GET /api/registrations). */
class SearchAcceptanceTest extends DefaultContextAcceptanceTest {

  static final String SEARCH = "/api/registrations";

  String organizerAuth() {
    return basicAuth(ORGANIZER_USERNAME, ORGANIZER_PASSWORD);
  }

  /** GET /api/registrations with the organizer's credentials and the given raw query string. */
  HttpResponse<String> search(String query) throws Exception {
    return get(SEARCH + (query.isEmpty() ? "" : "?" + query), "Authorization", organizerAuth());
  }

  static String enc(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  /** The body of a 200 answer. */
  JsonNode ok(HttpResponse<String> response) {
    assertThat(response.statusCode()).as("status, body: %s", response.body()).isEqualTo(200);
    return jsonBody(response);
  }

  static List<String> ids(JsonNode body) {
    List<String> ids = new ArrayList<>();
    for (JsonNode item : body.path("items")) {
      ids.add(item.path("id").asString());
    }
    return ids;
  }

  static JsonNode item(JsonNode body, String id) {
    for (JsonNode item : body.path("items")) {
      if (id.equals(item.path("id").asString())) {
        return item;
      }
    }
    throw new AssertionError("no item " + id + " in " + body);
  }

  static List<String> strings(JsonNode array) {
    List<String> values = new ArrayList<>();
    for (JsonNode v : array) {
      values.add(v.asString());
    }
    return values;
  }

  static Map<String, Object> external(String first, String last, String email, String org) {
    Map<String, Object> body = validExternal();
    body.put("firstName", first);
    body.put("lastName", last);
    body.put("email", email);
    body.put("organization", org);
    return body;
  }

  static Map<String, Object> student(String first, String last, String email, String inst) {
    Map<String, Object> body = validStudent();
    body.put("firstName", first);
    body.put("lastName", last);
    body.put("email", email);
    body.put("studyInstitution", inst);
    return body;
  }

  static Map<String, Object> withOptions(Map<String, Object> body, String... options) {
    body.put("options", new ArrayList<>(List.of(options)));
    return body;
  }

  String register(Map<String, Object> body) throws Exception {
    return createdId(postRegistration(body));
  }

  /** Three registrations, created in this order: Ana Novak, Luka Horvat (student), Maja Kranjc. */
  record Three(String ana, String luka, String maja) {}

  Three threeRegistrations() throws Exception {
    String ana =
        register(
            withOptions(
                external("Ana", "Novak", "ana.novak@example.org", "Example d.o.o."),
                "ws-ai",
                "ev-gala"));
    String luka =
        register(
            withOptions(
                student(
                    "Luka", "Horvat", "luka.horvat@student.example.org", "University of Ljubljana"),
                "ws-ai",
                "ev-student"));
    String maja =
        register(
            withOptions(external("Maja", "Kranjc", "maja@zeta.example", "Zeta Labs"), "ml-lunch"));
    return new Three(ana, luka, maja);
  }

  @Test
  @DisplayName("AC-009-01 without text or filters all registrations are listed newest first")
  void ac_009_01_listsAllNewestFirstWithFields() throws Exception {
    Three r = threeRegistrations();

    JsonNode body = ok(search(""));

    assertThat(body.path("total").asInt()).isEqualTo(3);
    assertThat(body.path("page").asInt()).isEqualTo(1);
    assertThat(ids(body)).containsExactly(r.maja(), r.luka(), r.ana());

    JsonNode ana = item(body, r.ana());
    assertThat(ana.path("type").asString()).isEqualTo("EXTERNAL");
    assertThat(ana.path("firstName").asString()).isEqualTo("Ana");
    assertThat(ana.path("lastName").asString()).isEqualTo("Novak");
    assertThat(ana.path("email").asString()).isEqualTo("ana.novak@example.org");
    assertThat(ana.path("organization").asString()).isEqualTo("Example d.o.o.");
    assertThat(
            ana.path("studyInstitution").isNull() || ana.path("studyInstitution").isMissingNode())
        .as("studyInstitution of an external registration: %s", ana)
        .isTrue();
    assertThat(strings(ana.path("options"))).containsExactlyInAnyOrder("ws-ai", "ev-gala");
    OffsetDateTime stored = (OffsetDateTime) registrationRow(r.ana()).get("registered_at");
    assertThat(OffsetDateTime.parse(ana.path("registeredAt").asString()))
        .isCloseTo(stored, within(1, ChronoUnit.MILLIS));

    JsonNode luka = item(body, r.luka());
    assertThat(luka.path("type").asString()).isEqualTo("STUDENT");
    assertThat(luka.path("firstName").asString()).isEqualTo("Luka");
    assertThat(luka.path("lastName").asString()).isEqualTo("Horvat");
    assertThat(luka.path("email").asString()).isEqualTo("luka.horvat@student.example.org");
    assertThat(luka.path("studyInstitution").asString()).isEqualTo("University of Ljubljana");
    assertThat(luka.path("organization").isNull() || luka.path("organization").isMissingNode())
        .as("organization of a student registration: %s", luka)
        .isTrue();
    assertThat(strings(luka.path("options"))).containsExactlyInAnyOrder("ws-ai", "ev-student");
    assertThat(OffsetDateTime.parse(luka.path("registeredAt").asString())).isNotNull();
  }

  @Test
  @DisplayName("AC-009-01 at most 20 registrations are listed, the total counts all of them")
  void ac_009_01_listsAtMostTwenty() throws Exception {
    List<String> created = new ArrayList<>();
    for (int i = 1; i <= 21; i++) {
      created.add(
          register(
              external("Many", "Person" + i, "many" + i + "@example.org", "Organization " + i)));
    }

    JsonNode body = ok(search(""));

    assertThat(body.path("total").asInt()).isEqualTo(21);
    assertThat(ids(body)).hasSize(20);
    assertThat(ids(body).get(0)).isEqualTo(created.get(20));
    assertThat(ids(body)).doesNotContain(created.get(0));
  }

  @ParameterizedTest(name = "AC-009-02 text \"{0}\" lists only {1}")
  @CsvSource(
      delimiter = '|',
      value = {
        "LUK|luka", // first name
        "nOvA|ana", // last name
        "@ZETA.EX|maja", // email
        "xample D.O|ana", // organization
        "of ljub|luka", // study institution
        "'  kranjc  '|maja", // leading and trailing whitespace is ignored (BR-10)
        "computer|", // study programme is not searched
        "63210001|" // student ID is not searched
      })
  void ac_009_02_textMatchesListedFieldsInAnyCase(String text, String expected) throws Exception {
    Three r = threeRegistrations();
    Map<String, String> byName = Map.of("ana", r.ana(), "luka", r.luka(), "maja", r.maja());

    JsonNode body = ok(search("text=" + enc(text)));

    List<String> want = expected == null ? List.of() : List.of(byName.get(expected));
    assertThat(ids(body)).containsExactlyElementsOf(want);
    assertThat(body.path("total").asInt()).isEqualTo(want.size());
  }

  @Test
  @DisplayName("AC-009-03 the type filter STUDENT lists only student registrations")
  void ac_009_03_typeFilterListsOnlyThatType() throws Exception {
    Three r = threeRegistrations();

    JsonNode students = ok(search("type=STUDENT"));
    assertThat(ids(students)).containsExactly(r.luka());
    assertThat(students.path("total").asInt()).isEqualTo(1);

    JsonNode externals = ok(search("type=EXTERNAL"));
    assertThat(ids(externals)).containsExactly(r.maja(), r.ana());
    assertThat(externals.path("total").asInt()).isEqualTo(2);
  }

  @Test
  @DisplayName("AC-009-04 the option filter lists only registrations that selected the option")
  void ac_009_04_optionFilterListsOnlyThoseWithTheOption() throws Exception {
    Three r = threeRegistrations();

    assertThat(ids(ok(search("option=ev-gala")))).containsExactly(r.ana());
    JsonNode workshop = ok(search("option=ws-ai"));
    assertThat(ids(workshop)).containsExactly(r.luka(), r.ana());
    assertThat(workshop.path("total").asInt()).isEqualTo(2);
    JsonNode nobody = ok(search("option=ot-tour"));
    assertThat(ids(nobody)).isEmpty();
    assertThat(nobody.path("total").asInt()).isZero();
  }

  @Test
  @DisplayName("AC-009-05 text, type and option filter together list only full matches")
  void ac_009_05_filtersCombine() throws Exception {
    String match =
        register(withOptions(external("Tina", "Petek", "tina@alpha.example", "Alpha"), "ws-ai"));
    // each of these misses exactly one of the three conditions
    register(withOptions(external("Tina", "Sever", "sever@beta.example", "Beta"), "ml-lunch"));
    register(withOptions(student("Tina", "Zorko", "zorko@uni.example", "FRI"), "ws-ai"));
    register(withOptions(external("Bor", "Kos", "bor@gamma.example", "Gamma"), "ws-ai"));

    JsonNode body = ok(search("text=tina&type=EXTERNAL&option=ws-ai"));

    assertThat(ids(body)).containsExactly(match);
    assertThat(body.path("total").asInt()).isEqualTo(1);
  }

  @Test
  @DisplayName("AC-009-06 no match gives a successful empty list with a total of 0")
  void ac_009_06_noMatchGivesEmptyList() throws Exception {
    threeRegistrations();

    JsonNode byText = ok(search("text=" + enc("no-such-participant-xyz")));
    assertThat(byText.path("items").isArray()).isTrue();
    assertThat(ids(byText)).isEmpty();
    assertThat(byText.path("total").asInt()).isZero();

    JsonNode byFilters = ok(search("text=novak&type=STUDENT"));
    assertThat(ids(byFilters)).isEmpty();
    assertThat(byFilters.path("total").asInt()).isZero();
  }

  @Test
  @DisplayName("AC-009-07 25 matches: page 1 lists the newest 20, page 2 the remaining 5")
  void ac_009_07_pagesOfTwenty() throws Exception {
    List<String> created = new ArrayList<>();
    for (int i = 1; i <= 25; i++) {
      created.add(
          register(external("Page", "Pager" + i, "pager" + i + "@example.org", "Paging Ltd")));
    }
    register(external("Other", "Person", "other@example.org", "Elsewhere"));
    List<String> newestFirst = new ArrayList<>(created).reversed();

    JsonNode page1 = ok(search("text=pager&page=1"));
    JsonNode page2 = ok(search("text=pager&page=2"));

    assertThat(page1.path("total").asInt()).isEqualTo(25);
    assertThat(page1.path("page").asInt()).isEqualTo(1);
    assertThat(ids(page1)).containsExactlyElementsOf(newestFirst.subList(0, 20));
    assertThat(page2.path("total").asInt()).isEqualTo(25);
    assertThat(page2.path("page").asInt()).isEqualTo(2);
    assertThat(ids(page2)).containsExactlyElementsOf(newestFirst.subList(20, 25));
  }

  @ParameterizedTest(name = "AC-009-08 {0} is rejected with an error for {1}")
  @CsvSource(
      delimiter = '|',
      value = {
        "type=VISITOR|type",
        "option=no-such-option|option",
        "page=0|page",
        "page=-1|page",
        "text=TEXT101|text"
      })
  void ac_009_08_invalidParameterIsRejected(String query, String field) throws Exception {
    threeRegistrations();
    String q = query.replace("TEXT101", "a".repeat(101));

    HttpResponse<String> response = search(q);

    assertThat(errorFields(response)).containsExactly(field);
    assertThat(response.body()).doesNotContain("Novak").doesNotContain("ana.novak@example.org");
  }

  @Test
  @DisplayName("AC-009-08 every invalid parameter gets its own error")
  void ac_009_08_allInvalidParametersAreReported() throws Exception {
    HttpResponse<String> response =
        search("text=" + "b".repeat(101) + "&type=VISITOR&option=no-such-option&page=0");

    assertThat(errorFields(response)).containsExactlyInAnyOrder("text", "type", "option", "page");
  }

  @Test
  @DisplayName("AC-009-08 a text of exactly 100 characters is accepted")
  void ac_009_08_hundredCharactersAreAccepted() throws Exception {
    JsonNode body = ok(search("text=" + "c".repeat(100)));

    assertThat(body.path("total").asInt()).isZero();
  }

  @ParameterizedTest(name = "AC-009-09 search with {0} is refused (401) without data")
  @ValueSource(strings = {"no credentials", "wrong password", "wrong user"})
  void ac_009_09_searchWithoutValidCredentialsIsRefused(String variant) throws Exception {
    Three r = threeRegistrations();
    String[] headers =
        switch (variant) {
          case "no credentials" -> new String[0];
          case "wrong password" ->
              new String[] {"Authorization", basicAuth(ORGANIZER_USERNAME, "wrong-password")};
          default -> new String[] {"Authorization", basicAuth("intruder", ORGANIZER_PASSWORD)};
        };

    HttpResponse<String> response = get(SEARCH, headers);

    assertThat(response.statusCode()).isEqualTo(401);
    assertThat(response.body())
        .doesNotContain("Novak")
        .doesNotContain("ana.novak@example.org")
        .doesNotContain(r.ana())
        .doesNotContain(r.luka());
  }

  @Test
  @DisplayName("AC-009-10 valid credentials over plain HTTP from a non-local client are refused")
  void ac_009_10_plainHttpFromRemoteClientIsRefused() throws Exception {
    Three r = threeRegistrations();

    // The request reaches the backend through a trusted local proxy that reports the client.
    HttpResponse<String> plain =
        get(
            SEARCH,
            "Authorization",
            organizerAuth(),
            "X-Forwarded-For",
            "203.0.113.7",
            "X-Forwarded-Proto",
            "http");

    assertThat(plain.statusCode()).isBetween(400, 499);
    assertThat(plain.body())
        .doesNotContain("Novak")
        .doesNotContain("ana.novak@example.org")
        .doesNotContain(r.ana());

    // Control: the same client over HTTPS (terminated by the proxy) is served.
    HttpResponse<String> secure =
        get(
            SEARCH,
            "Authorization",
            organizerAuth(),
            "X-Forwarded-For",
            "203.0.113.7",
            "X-Forwarded-Proto",
            "https");
    assertThat(ids(ok(secure))).contains(r.ana());
  }

  @ParameterizedTest(name = "AC-009-11 searching \"{0}\" lists the registration unchanged")
  @ValueSource(strings = {"Šuštaršič-Čeh Žagar", "šuštaršič-čeh žagar", "ČEH", "žag"})
  void ac_009_11_slovenianLastNameIsFoundUnchanged(String text) throws Exception {
    threeRegistrations();
    String id =
        register(
            external("Žiga Črtomir", "Šuštaršič-Čeh Žagar", "ziga@example.org", "Čebelica d.o.o."));

    JsonNode body = ok(search("text=" + enc(text)));

    assertThat(ids(body)).containsExactly(id);
    JsonNode found = item(body, id);
    assertThat(found.path("lastName").asString()).isEqualTo("Šuštaršič-Čeh Žagar");
    assertThat(found.path("firstName").asString()).isEqualTo("Žiga Črtomir");
    assertThat(found.path("organization").asString()).isEqualTo("Čebelica d.o.o.");
  }
}
