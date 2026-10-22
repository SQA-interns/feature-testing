package si.konferenca.registration.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.io.DefaultResourceLoader;
import si.konferenca.registration.domain.ConferenceCatalogue;
import si.konferenca.registration.domain.InvalidConfigurationException;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationType;
import tools.jackson.databind.json.JsonMapper;

class ConferenceConfigLoaderTest {

  @TempDir Path temp;

  private static final String CONSENTS =
      "\"consents\": [{\"id\": \"privacy\", \"text\": \"I agree.\", \"mandatory\": true}]";

  private static ConferenceCatalogue parse(String json) {
    return ConferenceConfigLoader.parse(JsonMapper.builder().build().readTree(json));
  }

  private static String withOption(String option) {
    return "{\"options\": [" + option + "], " + CONSENTS + "}";
  }

  @Test
  void theShippedConfigurationLoads() {
    ConferenceCatalogue catalogue =
        ConferenceConfigLoader.load(new DefaultResourceLoader(), "classpath:conference.json");

    assertThat(catalogue.mandatoryConsents()).extracting("id").containsExactly("privacy");
    for (OptionCategory category : OptionCategory.values()) {
      assertThat(catalogue.activeOptions())
          .as("an active option for externals in %s", category)
          .anyMatch(o -> o.category() == category && o.isOfferedTo(RegistrationType.EXTERNAL));
    }
  }

  @Test
  void aFileResourceLoadsAndMissingAvailabilityMeansBoth() throws IOException {
    Path file = temp.resolve("conference.json");
    Files.writeString(
        file,
        withOption(
            "{\"id\": \"ws\", \"name\": \"W\", \"category\": \"WORKSHOP\", \"active\": false}"),
        StandardCharsets.UTF_8);

    ConferenceCatalogue catalogue =
        ConferenceConfigLoader.load(new DefaultResourceLoader(), file.toUri().toString());

    assertThat(catalogue.options().getFirst().availableTo())
        .containsExactlyInAnyOrder(RegistrationType.EXTERNAL, RegistrationType.STUDENT);
    assertThat(catalogue.options().getFirst().active()).isFalse();
  }

  @Test
  void missingOrUnreadableFilesAreRefused() throws IOException {
    Path broken =
        Files.writeString(temp.resolve("broken.json"), "{ not json", StandardCharsets.UTF_8);

    assertThatThrownBy(
            () -> ConferenceConfigLoader.load(new DefaultResourceLoader(), "classpath:nope.json"))
        .isInstanceOf(InvalidConfigurationException.class)
        .hasMessageContaining("not found");
    assertThatThrownBy(
            () ->
                ConferenceConfigLoader.load(new DefaultResourceLoader(), broken.toUri().toString()))
        .hasMessageContaining("not valid JSON");
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(
      delimiter = '|',
      value = {
        "unknown category|{\"id\": \"ws\", \"name\": \"W\", \"category\": \"TRIP\", \"active\": true}|unknown category TRIP",
        "missing name|{\"id\": \"ws\", \"category\": \"OTHER\", \"active\": true}|missing text field name",
        "name not text|{\"id\": \"ws\", \"name\": 5, \"category\": \"OTHER\", \"active\": true}|missing text field name",
        "active not boolean|{\"id\": \"ws\", \"name\": \"W\", \"category\": \"OTHER\", \"active\": \"yes\"}|missing boolean field active",
        "unknown field|{\"id\": \"ws\", \"name\": \"W\", \"category\": \"OTHER\", \"active\": true, \"price\": 3}|unknown field price",
        "unknown type|{\"id\": \"ws\", \"name\": \"W\", \"category\": \"OTHER\", \"active\": true, \"availableTo\": [\"GUEST\"]}|unknown registration type",
        "type not text|{\"id\": \"ws\", \"name\": \"W\", \"category\": \"OTHER\", \"active\": true, \"availableTo\": [1]}|unknown registration type",
        "types not array|{\"id\": \"ws\", \"name\": \"W\", \"category\": \"OTHER\", \"active\": true, \"availableTo\": \"STUDENT\"}|availableTo must be a JSON array",
        "option not object|\"ws\"|option must be a JSON object"
      })
  void invalidOptionsAreRefused(String defect, String option, String message) {
    assertThatThrownBy(() -> parse(withOption(option)))
        .isInstanceOf(InvalidConfigurationException.class)
        .hasMessageContaining(message);
  }

  @Test
  void invalidDocumentShapesAreRefused() {
    assertThatThrownBy(() -> parse("[]"))
        .hasMessageContaining("configuration must be a JSON object");
    assertThatThrownBy(() -> parse("{\"options\": []}"))
        .hasMessageContaining("consents must be a JSON array");
    assertThatThrownBy(() -> parse("{\"options\": {}, " + CONSENTS + "}"))
        .hasMessageContaining("options must be a JSON array");
    assertThatThrownBy(() -> parse("{\"options\": [], " + CONSENTS + ", \"x\": 1}"))
        .hasMessageContaining("unknown field x");
    assertThatThrownBy(
            () -> parse("{\"options\": [], \"consents\": [{\"id\": \"p\", \"text\": \"T\"}]}"))
        .hasMessageContaining("missing boolean field mandatory");
  }

  @Test
  void duplicatesAreRefusedByTheCatalogue() {
    String option = "{\"id\": \"ws\", \"name\": \"W\", \"category\": \"OTHER\", \"active\": true}";

    assertThatThrownBy(() -> parse(withOption(option + ", " + option)))
        .hasMessageContaining("duplicate option id: ws");
  }
}
