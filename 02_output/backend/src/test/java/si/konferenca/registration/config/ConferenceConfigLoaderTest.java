package si.konferenca.registration.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import si.konferenca.registration.domain.ConferenceConfig;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationType;

class ConferenceConfigLoaderTest {

  static final String CONSENTS =
      "\"consents\":[{\"id\":\"privacy\",\"text\":\"t\",\"mandatory\":true}]";

  static ConferenceConfig parse(String json) {
    return ConferenceConfigLoader.parse(json.getBytes(StandardCharsets.UTF_8));
  }

  static String withOption(String option) {
    return "{\"conferenceName\":\"K\",\"options\":[" + option + "]," + CONSENTS + "}";
  }

  @Test
  void parsesOptionsWithDefaults() {
    ConferenceConfig config =
        parse(
            withOption(
                "{\"id\":\"ws-ai\",\"name\":\"AI\",\"category\":\"WORKSHOP\"},"
                    + "{\"id\":\"ev_1\",\"name\":\"E\",\"category\":\"EVENT\",\"active\":false,"
                    + "\"availableTo\":[\"STUDENT\"]}"));

    assertThat(config.conferenceName()).isEqualTo("K");
    ConferenceOption ai = config.option("ws-ai").orElseThrow();
    assertThat(ai.active()).isTrue();
    assertThat(ai.category()).isEqualTo(OptionCategory.WORKSHOP);
    assertThat(ai.availableTo())
        .containsExactlyInAnyOrder(RegistrationType.EXTERNAL, RegistrationType.STUDENT);
    ConferenceOption event = config.option("ev_1").orElseThrow();
    assertThat(event.active()).isFalse();
    assertThat(event.availableTo()).containsExactly(RegistrationType.STUDENT);
    assertThat(config.consents())
        .singleElement()
        .satisfies(c -> assertThat(c.mandatory()).isTrue());
  }

  @Test
  void loadsTheShippedClasspathFile() {
    ConferenceConfig config = ConferenceConfigLoader.load("classpath:conference.json");
    assertThat(config.activeOptions()).isNotEmpty();
    assertThat(ConferenceConfigLoader.load(" classpath:/conference.json ").conferenceName())
        .isEqualTo(config.conferenceName());
  }

  @Test
  void loadsAFile(@TempDir Path dir) throws Exception {
    Path file = dir.resolve("c.json");
    Files.writeString(file, withOption("{\"id\":\"a\",\"name\":\"A\",\"category\":\"MEAL\"}"));

    assertThat(ConferenceConfigLoader.load(file.toString()).option("a")).isPresent();
  }

  @Test
  void missingLocationsFail(@TempDir Path dir) {
    assertThatThrownBy(() -> ConferenceConfigLoader.load(null))
        .isInstanceOf(InvalidConferenceConfigException.class);
    assertThatThrownBy(() -> ConferenceConfigLoader.load("  "))
        .isInstanceOf(InvalidConferenceConfigException.class)
        .hasMessage("Invalid conference configuration: no conference configuration path is set");
    assertThatThrownBy(() -> ConferenceConfigLoader.load("classpath:missing.json"))
        .isInstanceOf(InvalidConferenceConfigException.class)
        .hasMessageContaining("configuration not found");
    assertThatThrownBy(() -> ConferenceConfigLoader.load(dir.resolve("none.json").toString()))
        .isInstanceOf(UncheckedIOException.class);
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "not json|not a valid configuration file",
        "{\"conferenceName\":\"K\",\"options\":[],\"consents\":[],\"extra\":1}|not a valid configuration file",
        "{\"conferenceName\":\"K\",\"options\":[],\"consents\":[]} {}|not a valid configuration file",
        "{\"conferenceName\":\"K\",\"consents\":[]}|conferenceName, options and consents are required",
        "{\"conferenceName\":\"K\",\"options\":[]}|conferenceName, options and consents are required",
        "null|conferenceName, options and consents are required",
        "{\"options\":[],\"consents\":[]}|conferenceName is missing",
        "{\"conferenceName\":\"K\",\"options\":[null],\"consents\":[]}|empty option entry",
        "{\"conferenceName\":\"K\",\"options\":[],\"consents\":[null]}|empty consent entry",
      })
  void invalidFilesFail(String json, String message) {
    assertThatThrownBy(() -> parse(json))
        .isInstanceOf(InvalidConferenceConfigException.class)
        .hasMessageContaining(message);
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "{\"name\":\"A\",\"category\":\"MEAL\"}|option id is missing or invalid",
        "{\"id\":\"-a\",\"name\":\"A\",\"category\":\"MEAL\"}|option id is missing or invalid",
        "{\"id\":\"a b\",\"name\":\"A\",\"category\":\"MEAL\"}|option id is missing or invalid",
        "{\"id\":\"a\",\"category\":\"MEAL\"}|name of option a is missing",
        "{\"id\":\"a\",\"name\":\" \",\"category\":\"MEAL\"}|name of option a is missing",
        "{\"id\":\"a\",\"name\":\"A\"}|unknown category 'null' of option a",
        "{\"id\":\"a\",\"name\":\"A\",\"category\":\"meal\"}|unknown category 'meal' of option a",
        "{\"id\":\"a\",\"name\":\"A\",\"category\":\"MEAL\",\"availableTo\":[]}|availableTo of option a is empty",
        "{\"id\":\"a\",\"name\":\"A\",\"category\":\"MEAL\",\"availableTo\":[\"GUEST\"]}|unknown availableTo 'GUEST' of option a",
        "{\"id\":\"a\",\"name\":\"A\",\"category\":\"MEAL\",\"availableTo\":[\"STUDENT\",\"STUDENT\"]}|availableTo of option a repeats",
      })
  void invalidOptionsFail(String option, String message) {
    assertThatThrownBy(() -> parse(withOption(option)))
        .isInstanceOf(InvalidConferenceConfigException.class)
        .hasMessage("Invalid conference configuration: " + message);
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "{\"text\":\"t\",\"mandatory\":true}|consent id is missing or invalid",
        "{\"id\":\"c\",\"mandatory\":true}|text of consent c is missing",
        "{\"id\":\"c\",\"text\":\"\",\"mandatory\":true}|text of consent c is missing",
        "{\"id\":\"c\",\"text\":\"t\"}|mandatory of consent c is missing",
      })
  void invalidConsentsFail(String consent, String message) {
    String json = "{\"conferenceName\":\"K\",\"options\":[],\"consents\":[" + consent + "]}";
    assertThatThrownBy(() -> parse(json))
        .isInstanceOf(InvalidConferenceConfigException.class)
        .hasMessage("Invalid conference configuration: " + message);
  }

  @Test
  void duplicateIdsFailWithTheDomainMessage() {
    String option = "{\"id\":\"a\",\"name\":\"A\",\"category\":\"MEAL\"}";
    assertThatThrownBy(() -> parse(withOption(option + "," + option)))
        .isInstanceOf(InvalidConferenceConfigException.class)
        .hasMessage("Invalid conference configuration: duplicate option id a")
        .hasCauseInstanceOf(IllegalArgumentException.class);
  }
}
