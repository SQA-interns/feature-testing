package si.konferenca.registration.adapter.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import si.konferenca.registration.domain.ConferenceCatalog;
import si.konferenca.registration.domain.ConferenceConfigException;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationType;

class JsonConferenceCatalogLoaderTest {

  private final JsonConferenceCatalogLoader loader = new JsonConferenceCatalogLoader();

  private ConferenceCatalog load(String json) {
    return loader.load(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), "test");
  }

  private static String config(String option, String consent) {
    return "{\"options\": [" + option + "], \"consents\": [" + consent + "]}";
  }

  private static final String CONSENT =
      "{\"id\": \"privacy\", \"text\": \"T\", \"mandatory\": true}";

  @Test
  void readsOptionsWithDefaultsAndConsents() {
    ConferenceCatalog catalog =
        load(
            config(
                "{\"id\": \"a\", \"name\": \"Ž A\", \"category\": \"MEAL\"},"
                    + "{\"id\": \"b\", \"name\": \"B\", \"category\": \"EVENT\", \"active\": false,"
                    + " \"availableTo\": [\"STUDENT\"]}",
                CONSENT + ", {\"id\": \"photos\", \"text\": \"P\", \"mandatory\": false}"));

    ConferenceOption a = catalog.option("a").orElseThrow();
    assertThat(a.name()).isEqualTo("Ž A");
    assertThat(a.category()).isEqualTo(OptionCategory.MEAL);
    assertThat(a.active()).isTrue();
    assertThat(a.availableTo()).containsExactlyInAnyOrder(RegistrationType.values());
    ConferenceOption b = catalog.option("b").orElseThrow();
    assertThat(b.active()).isFalse();
    assertThat(b.availableTo()).containsExactly(RegistrationType.STUDENT);
    assertThat(catalog.consents()).hasSize(2);
    assertThat(catalog.consent("privacy").orElseThrow().mandatory()).isTrue();
    assertThat(catalog.consent("photos").orElseThrow().mandatory()).isFalse();
  }

  @Test
  void rejectsAnUnknownCategoryNamingIt() {
    assertThatThrownBy(
            () ->
                load(config("{\"id\": \"a\", \"name\": \"A\", \"category\": \"SPORT\"}", CONSENT)))
        .isInstanceOf(ConferenceConfigException.class)
        .hasMessage("Option 'a' has the unknown category 'SPORT'");
  }

  @Test
  void rejectsAMissingCategory() {
    assertThatThrownBy(() -> load(config("{\"id\": \"a\", \"name\": \"A\"}", CONSENT)))
        .hasMessage("Option 'a' has no category");
  }

  @Test
  void rejectsAnUnknownRegistrationType() {
    assertThatThrownBy(
            () ->
                load(
                    config(
                        "{\"id\": \"a\", \"name\": \"A\", \"category\": \"MEAL\","
                            + " \"availableTo\": [\"GUEST\"]}",
                        CONSENT)))
        .hasMessage("Option 'a' names an unknown registration type \"GUEST\"");
    assertThatThrownBy(
            () ->
                load(
                    config(
                        "{\"id\": \"a\", \"name\": \"A\", \"category\": \"MEAL\","
                            + " \"availableTo\": [1]}",
                        CONSENT)))
        .hasMessageStartingWith("Option 'a' names an unknown registration type");
  }

  @Test
  void rejectsAnEmptyAvailability() {
    assertThatThrownBy(
            () ->
                load(
                    config(
                        "{\"id\": \"a\", \"name\": \"A\", \"category\": \"MEAL\", \"availableTo\": []}",
                        CONSENT)))
        .hasMessage("Option 'a' is available to no registration type");
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "{\"options\": [], \"consents\": [], \"extra\": 1}|Unknown field 'extra' in the configuration",
        "[]|Expected an object for the configuration",
        "{\"consents\": []}|Field 'options' must be an array",
        "{\"options\": {}, \"consents\": []}|Field 'options' must be an array",
        "{\"options\": [1], \"consents\": []}|Expected an object for an option",
        "{\"options\": [], \"consents\": [\"x\"]}|Expected an object for a consent",
        "{\"options\": [{\"id\": \"a\", \"name\": \"A\", \"category\": \"MEAL\", \"price\": 3}],"
            + " \"consents\": []}|Unknown field 'price' in an option",
        "{\"options\": [{\"id\": 7, \"name\": \"A\", \"category\": \"MEAL\"}], \"consents\": []}"
            + "|Field 'id' must be a string",
        "{\"options\": [{\"id\": \"a\", \"name\": \"A\", \"category\": \"MEAL\", \"active\": \"yes\"}],"
            + " \"consents\": []}|Field 'active' must be true or false",
        "{\"options\": [], \"consents\": [{\"id\": \"c\", \"text\": \"T\"}]}"
            + "|Field 'mandatory' must be true or false",
        "{\"options\": [], \"consents\": [{\"id\": \"c\", \"text\": \"T\", \"mandatory\": true,"
            + " \"x\": 1}]}|Unknown field 'x' in a consent",
        "{\"options\": [{\"id\": \"a\", \"name\": null, \"category\": \"MEAL\"}], \"consents\": []}"
            + "|Missing name of option 'a'"
      })
  void rejectsMalformedConfigurations(String json, String message) {
    assertThatThrownBy(() -> load(json))
        .isInstanceOf(ConferenceConfigException.class)
        .hasMessage(message);
  }

  @Test
  void rejectsUnreadableJson() {
    assertThatThrownBy(() -> load("{ not json"))
        .isInstanceOf(ConferenceConfigException.class)
        .hasMessage("Conference configuration test is unreadable")
        .hasCauseInstanceOf(RuntimeException.class);
  }
}
