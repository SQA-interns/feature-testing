package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** US-009 Organizer registration search (AC-009-01..09, 11; AC-009-10 in OrganizerHttpsOnly). */
class Us009SearchAcceptanceTest extends AcceptanceTestBase {

  /** GET /api/registrations as organizer with the given query string; asserts HTTP 200. */
  private JsonNode search(String query) {
    HttpResponse<byte[]> response =
        getAsOrganizer("/api/registrations" + (query.isEmpty() ? "" : "?" + query));
    assertThat(response.statusCode()).as(query + " -> " + text(response)).isEqualTo(200);
    return json(response);
  }

  private static List<String> ids(JsonNode result) {
    List<String> ids = new ArrayList<>();
    result.path("items").forEach(i -> ids.add(i.path("id").asString()));
    return ids;
  }

  private static List<String> strings(JsonNode array) {
    List<String> values = new ArrayList<>();
    array.forEach(v -> values.add(v.asString()));
    return values;
  }

  private static JsonNode item(JsonNode result, String id) {
    for (JsonNode i : result.path("items")) {
      if (i.path("id").asString().equals(id)) {
        return i;
      }
    }
    return null;
  }

  private String registerExternal(String lastName, String... optionIds) {
    ObjectNode body = options(validExternal(), optionIds);
    body.put("lastName", lastName);
    return register(body);
  }

  private String registerStudent(String lastName, String... optionIds) {
    ObjectNode body = options(validStudent(), optionIds);
    body.put("lastName", lastName);
    return register(body);
  }

  @DisplayName("AC-009-01")
  @Test
  void ac_009_01_noFiltersListsNewestFirstWithFieldsAndTotal() {
    long totalBefore = search("").path("total").asLong();
    ObjectNode external = options(validExternal(), "ws-ai", "other-tour");
    String first = register(external);
    ObjectNode student = options(validStudent(), "meal-lunch");
    String second = register(student);
    String third = registerExternal("Third");

    JsonNode result = search("");

    assertThat(result.path("total").asLong()).isEqualTo(totalBefore + 3);
    assertThat(result.path("page").asInt()).isEqualTo(1);
    List<String> ids = ids(result);
    assertThat(ids.size()).isLessThanOrEqualTo(20);
    assertThat(ids.subList(0, 3)).containsExactly(third, second, first);

    JsonNode e = item(result, first);
    assertThat(e.path("type").asString()).isEqualTo("EXTERNAL");
    assertThat(e.path("firstName").asString()).isEqualTo("Ana");
    assertThat(e.path("lastName").asString()).isEqualTo("Novak");
    assertThat(e.path("email").asString()).isEqualTo(external.path("email").asString());
    assertThat(e.path("organization").asString()).isEqualTo("Example d.o.o.");
    assertThat(strings(e.path("options"))).containsExactlyInAnyOrder("ws-ai", "other-tour");
    assertThat(OffsetDateTime.parse(e.path("registeredAt").asString())).isNotNull();

    JsonNode s = item(result, second);
    assertThat(s.path("type").asString()).isEqualTo("STUDENT");
    assertThat(s.path("studyInstitution").asString()).isEqualTo("University of Ljubljana");
    assertThat(strings(s.path("options"))).containsExactly("meal-lunch");
    assertThat(s.path("email").asString()).isEqualTo(student.path("email").asString());
  }

  @DisplayName("AC-009-01")
  @Test
  void ac_009_01_atMostTwentyItemsArePerPage() {
    for (int i = 0; i < 21; i++) {
      registerExternal("Bulk" + i);
    }

    JsonNode result = search("");

    assertThat(result.path("items").size()).isEqualTo(20);
    assertThat(result.path("total").asLong()).isGreaterThanOrEqualTo(21);
  }

  @DisplayName("AC-009-02")
  @Test
  void ac_009_02_textMatchesAnyPartOfNameEmailOrganizationOrInstitutionInAnyCase() {
    String token = token();
    String upper = token.toUpperCase();
    ObjectNode byFirst = validExternal();
    byFirst.put("firstName", "Mar" + upper + "ko");
    ObjectNode byLast = validStudent();
    byLast.put("lastName", "Kr" + token + "anjc");
    ObjectNode byEmail = validExternal();
    byEmail.put("email", "x" + token + "y@participants.example");
    ObjectNode byOrganization = validExternal();
    byOrganization.put("organization", "Firma " + upper + " d.o.o.");
    ObjectNode byInstitution = validStudent();
    byInstitution.put("studyInstitution", "Univerza " + token);
    ObjectNode noMatch = validStudent();
    noMatch.put("studyProgramme", "Programme " + token);
    noMatch.put("studentId", token);

    List<String> expected = new ArrayList<>();
    for (ObjectNode body : List.of(byFirst, byLast, byEmail, byOrganization, byInstitution)) {
      expected.add(register(body));
    }
    String notExpected = register(noMatch);

    String mixedCase = token.substring(0, 4).toUpperCase() + token.substring(4);
    JsonNode result = search("text=" + encode(mixedCase));

    assertThat(ids(result)).containsExactlyInAnyOrderElementsOf(expected);
    assertThat(ids(result)).doesNotContain(notExpected);
    assertThat(result.path("total").asLong()).isEqualTo(5);
  }

  @DisplayName("AC-009-02")
  @Test
  void ac_009_02_surroundingWhitespaceOfTheTextIsIgnored() {
    String token = token();
    String id = registerExternal("Lastname" + token);

    JsonNode result = search("text=" + encode("  " + token.toUpperCase() + "  "));

    assertThat(ids(result)).containsExactly(id);
  }

  @DisplayName("AC-009-03")
  @Test
  void ac_009_03_typeFilterStudentListsOnlyStudents() {
    String token = token();
    registerExternal("Ext" + token);
    String student = registerStudent("Stu" + token);

    JsonNode filtered = search("text=" + token + "&type=STUDENT");
    JsonNode onlyType = search("type=STUDENT");

    assertThat(ids(filtered)).containsExactly(student);
    assertThat(filtered.path("total").asLong()).isEqualTo(1);
    assertThat(onlyType.path("items").size()).isPositive();
    onlyType.path("items").forEach(i -> assertThat(i.path("type").asString()).isEqualTo("STUDENT"));
    assertThat(onlyType.path("total").asLong()).isLessThan(search("").path("total").asLong());
  }

  @DisplayName("AC-009-04")
  @Test
  void ac_009_04_optionFilterListsOnlyRegistrationsWithThatOption() {
    String token = token();
    String withTour = registerExternal("Tour" + token, "other-tour", "ws-ai");
    registerExternal("NoTour" + token, "ws-ai");
    registerStudent("StudentNoTour" + token);

    JsonNode filtered = search("text=" + token + "&option=other-tour");
    JsonNode onlyOption = search("option=other-tour");

    assertThat(ids(filtered)).containsExactly(withTour);
    assertThat(onlyOption.path("items").size()).isPositive();
    onlyOption
        .path("items")
        .forEach(i -> assertThat(strings(i.path("options"))).contains("other-tour"));
  }

  @DisplayName("AC-009-05")
  @Test
  void ac_009_05_textTypeAndOptionFiltersCombine() {
    String token = token();
    registerExternal("A" + token, "ws-ai");
    String match = registerStudent("B" + token, "ws-ai");
    registerStudent("C" + token, "meal-lunch");
    registerStudent("Other" + token() + "x", "ws-ai");

    JsonNode result = search("text=" + token + "&type=STUDENT&option=ws-ai");

    assertThat(ids(result)).containsExactly(match);
    assertThat(result.path("total").asLong()).isEqualTo(1);
  }

  @DisplayName("AC-009-06")
  @Test
  void ac_009_06_noMatchIsSuccessfulWithEmptyListAndZeroTotal() {
    registerExternal("Someone");

    JsonNode result = search("text=" + token() + "nomatch");

    assertThat(result.path("items").isArray()).isTrue();
    assertThat(result.path("items").size()).isZero();
    assertThat(result.path("total").asLong()).isZero();
  }

  @DisplayName("AC-009-07")
  @Test
  void ac_009_07_twentyFiveMatchesArePagedTwentyThenFive() {
    String token = token();
    List<String> created = new ArrayList<>();
    for (int i = 0; i < 25; i++) {
      ObjectNode body = validExternal();
      body.put("organization", "Paging " + token + " " + i);
      created.add(register(body));
    }
    List<String> newestFirst = new ArrayList<>(created);
    java.util.Collections.reverse(newestFirst);

    JsonNode page1 = search("text=" + token + "&page=1");
    JsonNode page2 = search("text=" + token + "&page=2");
    JsonNode defaultPage = search("text=" + token);

    assertThat(ids(page1)).containsExactlyElementsOf(newestFirst.subList(0, 20));
    assertThat(ids(page2)).containsExactlyElementsOf(newestFirst.subList(20, 25));
    assertThat(page1.path("total").asLong()).isEqualTo(25);
    assertThat(page2.path("total").asLong()).isEqualTo(25);
    assertThat(page2.path("page").asInt()).isEqualTo(2);
    assertThat(ids(defaultPage)).isEqualTo(ids(page1));
  }

  @DisplayName("AC-009-08")
  @ParameterizedTest(name = "AC-009-08 {0}={1}")
  @CsvSource({
    "type, FOO",
    "option, no-such-option",
    "page, 0",
    "page, -1",
    "page, abc",
    "text, "
        + "abcdefghij"
        + "abcdefghij"
        + "abcdefghij"
        + "abcdefghij"
        + "abcdefghij"
        + "abcdefghij"
        + "abcdefghij"
        + "abcdefghij"
        + "abcdefghij"
        + "abcdefghij"
        + "x"
  })
  void ac_009_08_invalidParameterIsRejectedWithErrorForThatParameter(String name, String value) {
    HttpResponse<byte[]> response = getAsOrganizer("/api/registrations?" + name + "=" + value);

    assertFieldError(response, name);
  }

  @DisplayName("AC-009-08")
  @Test
  void ac_009_08_boundaryValuesAreAccepted() {
    String hundred = "a".repeat(100);

    assertThat(getAsOrganizer("/api/registrations?text=" + hundred).statusCode()).isEqualTo(200);
    assertThat(getAsOrganizer("/api/registrations?page=1&type=EXTERNAL").statusCode())
        .isEqualTo(200);
    assertThat(getAsOrganizer("/api/registrations?option=meal-veg").statusCode()).isEqualTo(200);
  }

  @DisplayName("AC-009-09")
  @Test
  void ac_009_09_searchWithoutOrWithWrongCredentialsIsRefused() {
    ObjectNode body = validExternal();
    String email = body.path("email").asString();
    register(body);
    String path = "/api/registrations?text=" + encode(email);

    HttpResponse<byte[]> anonymous = get(path);
    HttpResponse<byte[]> wrongPassword = getWithBasic(path, ORGANIZER_USERNAME, "wrong-password");
    HttpResponse<byte[]> wrongUser = getWithBasic(path, "nobody", ORGANIZER_PASSWORD);

    for (HttpResponse<byte[]> response : List.of(anonymous, wrongPassword, wrongUser)) {
      assertThat(response.statusCode()).isEqualTo(401);
      assertThat(text(response)).doesNotContain(email).doesNotContain("Novak");
    }
    assertThat(text(getAsOrganizer(path))).contains(email);
  }

  @DisplayName("AC-009-11")
  @Test
  void ac_009_11_slovenianLastNameIsFoundAndUnchanged() {
    String lastName = "Čšžćevič-ŠŽČ " + token();
    String id = registerStudent(lastName);

    JsonNode exact = search("text=" + encode(lastName));
    JsonNode partial = search("text=" + encode("čšžć"));

    assertThat(ids(exact)).containsExactly(id);
    assertThat(item(exact, id).path("lastName").asString()).isEqualTo(lastName);
    assertThat(ids(partial)).contains(id);
  }
}
