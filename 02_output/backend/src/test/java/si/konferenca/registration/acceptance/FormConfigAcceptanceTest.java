package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import si.konferenca.registration.acceptance.support.AcceptanceTest;
import si.konferenca.registration.acceptance.support.TestStack;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** US-003: {@code GET /api/form-config} lists only the active configured options. */
@AcceptanceTest
class FormConfigAcceptanceTest {

  @Autowired private MockMvc mvc;

  @DynamicPropertySource
  static void stack(DynamicPropertyRegistry registry) {
    TestStack.register(registry);
  }

  @Test
  void ac_003_01_formConfigListsOnlyActiveOptionsWithTheirAttributes() throws Exception {
    JsonNode config = formConfig(mvc);

    Map<String, JsonNode> options = optionsById(config);
    assertThat(options.keySet())
        .containsExactlyInAnyOrder(
            "ws-ai", "ws-data", "ev-gala", "ev-tour", "meal-lunch", "other-cert");
    assertThat(options).doesNotContainKey("ws-old");

    JsonNode tour = options.get("ev-tour");
    assertThat(tour.path("name").asString("")).isEqualTo("Industry tour");
    assertThat(tour.path("category").asString("")).isEqualTo("EVENT");
    assertThat(strings(tour.path("availableTo"))).containsExactly("EXTERNAL");

    JsonNode lunch = options.get("meal-lunch");
    assertThat(lunch.path("name").asString("")).isEqualTo("Lunch");
    assertThat(lunch.path("category").asString("")).isEqualTo("MEAL");
    assertThat(strings(lunch.path("availableTo"))).containsExactlyInAnyOrder("EXTERNAL", "STUDENT");

    assertThat(options.get("other-cert").path("category").asString("")).isEqualTo("OTHER");
    assertThat(options.get("ws-ai").path("category").asString("")).isEqualTo("WORKSHOP");
  }

  @Test
  void ac_003_01_optionWithoutConfiguredTypesIsAvailableToBoth() throws Exception {
    JsonNode data = optionsById(formConfig(mvc)).get("ws-data");

    assertThat(strings(data.path("availableTo"))).containsExactlyInAnyOrder("EXTERNAL", "STUDENT");
  }

  @Test
  void ac_003_01_formConfigListsTheConfiguredConsents() throws Exception {
    JsonNode consents = formConfig(mvc).path("consents");

    Map<String, Boolean> mandatory = new HashMap<>();
    consents
        .values()
        .forEach(c -> mandatory.put(c.path("id").asString(""), c.path("mandatory").asBoolean()));
    assertThat(mandatory).containsEntry("privacy", true).containsEntry("photos", false);
  }

  static JsonNode formConfig(MockMvc mvc) throws Exception {
    byte[] body =
        mvc.perform(get("/api/form-config"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    return JsonMapper.builder().build().readTree(new String(body, StandardCharsets.UTF_8));
  }

  static Map<String, JsonNode> optionsById(JsonNode config) {
    Map<String, JsonNode> options = new HashMap<>();
    config.path("options").values().forEach(o -> options.put(o.path("id").asString(""), o));
    return options;
  }

  static List<String> strings(JsonNode array) {
    return array.values().stream().map(n -> n.asString("")).toList();
  }
}
