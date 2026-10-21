package si.konferenca.registration.acceptance.support;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Builds request bodies of {@code POST /api/registrations} ({@code docs/02_contracts/openapi.json})
 * and reads responses. A {@code null} value in the map is sent as JSON null; a removed key is
 * absent.
 */
public final class Submissions {

  public static final String CAPTCHA_PASS = "test-pass";

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private Submissions() {}

  /** A unique address, so a test can find exactly its own row, copy and emails. */
  public static String uniqueEmail(String name) {
    return name + "." + UUID.randomUUID().toString().substring(0, 8) + "@example.org";
  }

  /** A valid external submission with the options {@code ws-ai} and {@code ev-gala}. */
  public static Map<String, Object> validExternal() {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", "EXTERNAL");
    body.put("firstName", "Ana");
    body.put("lastName", "Novak");
    body.put("email", uniqueEmail("ana"));
    body.put("organization", "Example d.o.o.");
    body.put("studyInstitution", null);
    body.put("studyProgramme", null);
    body.put("studentId", null);
    body.put("options", new ArrayList<>(List.of("ws-ai", "ev-gala")));
    body.put("consents", new ArrayList<>(List.of("privacy")));
    body.put("captchaToken", CAPTCHA_PASS);
    return body;
  }

  /** A valid student submission with the options {@code ws-data} and {@code meal-lunch}. */
  public static Map<String, Object> validStudent() {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", "STUDENT");
    body.put("firstName", "Luka");
    body.put("lastName", "Kranjc");
    body.put("email", uniqueEmail("luka"));
    body.put("organization", null);
    body.put("studyInstitution", "Univerza v Ljubljani");
    body.put("studyProgramme", "Računalništvo in informatika");
    body.put("studentId", "63210001");
    body.put("options", new ArrayList<>(List.of("ws-data", "meal-lunch")));
    body.put("consents", new ArrayList<>(List.of("privacy")));
    body.put("captchaToken", CAPTCHA_PASS);
    return body;
  }

  public static MockHttpServletRequestBuilder post(Map<String, Object> body) {
    return MockMvcRequestBuilders.post("/api/registrations")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(JSON.writeValueAsString(body).getBytes(StandardCharsets.UTF_8));
  }

  public static JsonNode body(MvcResult result) {
    return JSON.readTree(
        new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8));
  }

  /** The {@code id} of a 201 response. */
  public static String id(MvcResult result) {
    return body(result).path("id").asString("");
  }

  /** The {@code field} values of a 400 response's {@code errors}. */
  public static List<String> errorFields(MvcResult result) {
    return body(result).path("errors").values().stream()
        .map(e -> e.path("field").asString(""))
        .toList();
  }

  public static MockHttpServletRequestBuilder export(String username, String password) {
    MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/api/export");
    if (username != null) {
      String token = username + ":" + password;
      request.header(
          HttpHeaders.AUTHORIZATION,
          "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8)));
    }
    return request;
  }

  public static MockHttpServletRequestBuilder export() {
    return export(TestStack.ORGANIZER_USERNAME, TestStack.ORGANIZER_PASSWORD);
  }
}
