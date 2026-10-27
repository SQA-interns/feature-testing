package si.konferenca.registration.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import si.konferenca.registration.domain.ConferenceCatalog;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationType;

class ConferenceConfigLoaderTest {

  private static final String CONSENTS =
      "\"consents\":[{\"id\":\"privacy\",\"text\":\"I agree\",\"mandatory\":true}]";

  @TempDir Path dir;

  private Path write(String json) throws IOException {
    Path file = dir.resolve("conference.json");
    Files.writeString(file, json.replace('\'', '"'));
    return file;
  }

  private ConferenceCatalog load(String json) throws IOException {
    return new ConferenceConfigLoader().load(write(json));
  }

  private static String withOption(String option) {
    return "{\"options\":[" + option + "]," + CONSENTS + "}";
  }

  @Test
  void loadsOptionsAndConsentsInOrder() throws IOException {
    ConferenceCatalog catalog =
        load(
            "{'options':[{'id':'ws-a','name':'Workshop A','category':'WORKSHOP','active':true},"
                + "{'id':'ev-1','name':'Dinner','category':'EVENT','active':false,"
                + "'availableTo':['STUDENT']}],"
                + "'consents':[{'id':'privacy','text':'I agree','mandatory':true},"
                + "{'id':'photos','text':'Photos','mandatory':false}]}");

    assertThat(catalog.options())
        .containsExactly(
            new ConferenceOption(
                "ws-a",
                "Workshop A",
                OptionCategory.WORKSHOP,
                true,
                Set.of(RegistrationType.EXTERNAL, RegistrationType.STUDENT)),
            new ConferenceOption(
                "ev-1", "Dinner", OptionCategory.EVENT, false, Set.of(RegistrationType.STUDENT)));
    assertThat(catalog.consents())
        .containsExactly(
            new ConsentDefinition("privacy", "I agree", true),
            new ConsentDefinition("photos", "Photos", false));
  }

  @Test
  void acceptsEmptyListsAndLimitValues() throws IOException {
    assertThat(load("{'options':[],'consents':[]}").options()).isEmpty();
    String id = "a" + "b".repeat(63);
    ConferenceCatalog catalog =
        load(
            withOption(
                "{'id':'"
                    + id
                    + "','name':'"
                    + "ž".repeat(200)
                    + "','category':'MEAL','active':true,'availableTo':['EXTERNAL','STUDENT']}"));
    assertThat(catalog.option(id)).isPresent();
    assertThat(
            load("{'options':[],'consents':[{'id':'c','text':'"
                    + "x".repeat(1000)
                    + "','mandatory':false}]}")
                .consents())
        .hasSize(1);
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "[]| $: must be an object",
        "{'consents':[]}| $.options: is missing",
        "{'options':[]}| $.consents: is missing",
        "{'options':[],'consents':[],'x':1}| $.x: is not allowed",
        "{'options':{},'consents':[]}| options: must be an array",
        "{'options':[],'consents':'x'}| consents: must be an array",
        "{'options':[1],'consents':[]}| options[0]: must be an object",
        "{'options':[],'consents':[{'id':'c','text':'t'}]}| consents[0].mandatory: is missing",
        "{'options':[],'consents':[{'id':'c','text':' ','mandatory':true}]}"
            + "| consents[0].text: must be a non-blank string",
        "{'options':[],'consents':[{'id':'c','text':'t','mandatory':'yes'}]}"
            + "| consents[0].mandatory: must be true or false",
        "{'options':[],'consents':[{'id':'C','text':'t','mandatory':true}]}"
            + "| consents[0].id: must match [a-z0-9][a-z0-9-]{0,63}",
        "{'options':[],'consents':[{'id':'c','text':'t','mandatory':true},"
            + "{'id':'c','text':'u','mandatory':true}]}| duplicate consent id: c",
      })
  void rejectsInvalidStructure(String json, String problem) {
    assertThatThrownBy(() -> load(json))
        .isInstanceOf(InvalidConferenceConfigException.class)
        .hasMessageEndingWith("conference.json: " + problem);
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "{'name':'n','category':'MEAL','active':true}| options[0].id: is missing",
        "{'id':'a','category':'MEAL','active':true}| options[0].name: is missing",
        "{'id':'a','name':'n','active':true}| options[0].category: is missing",
        "{'id':'a','name':'n','category':'MEAL'}| options[0].active: is missing",
        "{'id':'a','name':'n','category':'MEAL','active':true,'price':1}"
            + "| options[0].price: is not allowed",
        "{'id':'-a','name':'n','category':'MEAL','active':true}"
            + "| options[0].id: must match [a-z0-9][a-z0-9-]{0,63}",
        "{'id':1,'name':'n','category':'MEAL','active':true}"
            + "| options[0].id: must match [a-z0-9][a-z0-9-]{0,63}",
        "{'id':'a','name':1,'category':'MEAL','active':true}"
            + "| options[0].name: must be a non-blank string",
        "{'id':'a','name':'n','category':'meal','active':true}"
            + "| options[0].category: must be one of WORKSHOP, EVENT, MEAL, OTHER",
        "{'id':'a','name':'n','category':1,'active':true}"
            + "| options[0].category: must be one of WORKSHOP, EVENT, MEAL, OTHER",
        "{'id':'a','name':'n','category':'MEAL','active':1}"
            + "| options[0].active: must be true or false",
        "{'id':'a','name':'n','category':'MEAL','active':true,'availableTo':'STUDENT'}"
            + "| options[0].availableTo: must be an array",
        "{'id':'a','name':'n','category':'MEAL','active':true,'availableTo':[]}"
            + "| options[0].availableTo: must name at least one registration type",
        "{'id':'a','name':'n','category':'MEAL','active':true,'availableTo':['ALUMNI']}"
            + "| options[0].availableTo: unknown or repeated registration type",
        "{'id':'a','name':'n','category':'MEAL','active':true,'availableTo':[1]}"
            + "| options[0].availableTo: unknown or repeated registration type",
        "{'id':'a','name':'n','category':'MEAL','active':true,'availableTo':['STUDENT','STUDENT']}"
            + "| options[0].availableTo: unknown or repeated registration type",
      })
  void rejectsInvalidOptions(String option, String problem) {
    assertThatThrownBy(() -> load(withOption(option)))
        .isInstanceOf(InvalidConferenceConfigException.class)
        .hasMessageEndingWith("conference.json: " + problem);
  }

  @Test
  void rejectsTooLongIdsAndTexts() {
    String longId = "a".repeat(65);
    assertThatThrownBy(
            () ->
                load(
                    withOption(
                        "{'id':'" + longId + "','name':'n','category':'MEAL','active':true}")))
        .hasMessageContaining("options[0].id: must match");
    assertThatThrownBy(
            () ->
                load(
                    withOption(
                        "{'id':'a','name':'"
                            + "x".repeat(201)
                            + "','category':'MEAL','active':true}")))
        .hasMessageEndingWith("options[0].name: is longer than 200");
    assertThatThrownBy(
            () ->
                load(
                    "{'options':[],'consents':[{'id':'c','text':'"
                        + "x".repeat(1001)
                        + "','mandatory':true}]}"))
        .hasMessageEndingWith("consents[0].text: is longer than 1000");
    assertThatThrownBy(
            () ->
                load(
                    "{'options':[{'id':'a','name':'n','category':'MEAL','active':true},"
                        + "{'id':'a','name':'m','category':'MEAL','active':true}],'consents':[]}"))
        .hasMessageEndingWith("duplicate option id: a");
  }

  @Test
  void rejectsUnreadableFiles() throws IOException {
    Path missing = dir.resolve("missing.json");
    assertThatThrownBy(() -> new ConferenceConfigLoader().load(missing))
        .isInstanceOf(InvalidConferenceConfigException.class)
        .hasMessage("conference configuration " + missing + ": cannot be read as JSON");
    assertThatThrownBy(() -> load("{not json"))
        .hasMessageEndingWith("conference.json: cannot be read as JSON");
  }
}
