package si.konferenca.registration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import si.konferenca.registration.domain.ConferenceConfiguration;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;
import si.konferenca.registration.domain.FieldError;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;
import si.konferenca.registration.domain.SearchCriteria;

/** ExportService, SearchService and FormConfigurationService. */
class QueryServicesTest {

  private static final ConferenceConfiguration CONFIG =
      new ConferenceConfiguration(
          List.of(
              new ConferenceOption(
                  "a", "A", OptionCategory.MEAL, true, Set.of(RegistrationType.STUDENT)),
              new ConferenceOption(
                  "b", "B", OptionCategory.MEAL, false, Set.of(RegistrationType.STUDENT))),
          List.of(new ConsentDefinition("privacy", "I agree.", true)));

  private static final Instant AT = Instant.parse("2026-10-20T10:15:00Z");

  private final RegistrationStore store = mock(RegistrationStore.class);

  private static Registration external() {
    return new Registration(
        UUID.fromString("00000000-0000-4000-8000-000000000001"),
        RegistrationType.EXTERNAL,
        "Ana",
        "Novak",
        "ana@example.org",
        "Org",
        null,
        null,
        null,
        List.of("a", "b"),
        List.of(
            new Registration.GivenConsent("privacy", AT),
            new Registration.GivenConsent("photo", AT)),
        AT);
  }

  private static Registration student() {
    return new Registration(
        UUID.fromString("00000000-0000-4000-8000-000000000002"),
        RegistrationType.STUDENT,
        "Luka",
        "Kranjc",
        "luka@uni.si",
        null,
        "Uni",
        "CS",
        "123",
        List.of(),
        List.of(),
        AT);
  }

  @Test
  void exportRowsHaveTheContractColumnsWithEmptyCellsForTheOtherType() {
    assertThat(ExportService.row(external()))
        .containsExactly(
            "00000000-0000-4000-8000-000000000001",
            "EXTERNAL",
            "Ana",
            "Novak",
            "ana@example.org",
            "Org",
            "",
            "",
            "",
            "a, b",
            "privacy, photo",
            "2026-10-20T10:15:00Z");
    assertThat(ExportService.row(student()))
        .containsExactly(
            "00000000-0000-4000-8000-000000000002",
            "STUDENT",
            "Luka",
            "Kranjc",
            "luka@uni.si",
            "",
            "Uni",
            "CS",
            "123",
            "",
            "",
            "2026-10-20T10:15:00Z");
  }

  @Test
  @SuppressWarnings("unchecked")
  void exportWritesOneRowPerStoredRegistrationOnTheRegistrationsSheet() {
    WorkbookWriter writer = mock(WorkbookWriter.class);
    when(store.findAllOldestFirst()).thenReturn(List.of(external(), student()));
    when(writer.write(any(), any(), any())).thenReturn(new byte[] {9});

    byte[] workbook = new ExportService(store, writer).exportWorkbook();

    assertThat(workbook).containsExactly(9);
    ArgumentCaptor<List<List<String>>> rows = ArgumentCaptor.forClass(List.class);
    verify(writer).write(eq("Registrations"), eq(ExportService.COLUMNS), rows.capture());
    assertThat(rows.getValue()).hasSize(2);
    assertThat(rows.getValue().get(1)).isEqualTo(ExportService.row(student()));
    assertThat(ExportService.COLUMNS).hasSize(12).startsWith("id", "type").endsWith("registeredAt");
  }

  @Test
  void searchWithInvalidParametersReturnsErrorsWithoutQuerying() {
    SearchService.Result result = new SearchService(store, CONFIG).search(null, "X", null, "0");

    assertThat(result.results()).isNull();
    assertThat(result.page()).isZero();
    assertThat(result.errors()).extracting(FieldError::field).containsExactly("type", "page");
    verifyNoInteractions(store);
  }

  @Test
  void searchPassesTheParsedCriteriaAndReturnsThePage() {
    SearchPage page = new SearchPage(21, List.of(student()));
    when(store.search(any())).thenReturn(page);

    SearchService.Result result =
        new SearchService(store, CONFIG).search(" Kr ", "STUDENT", "b", "2");

    assertThat(result.errors()).isEmpty();
    assertThat(result.page()).isEqualTo(2);
    assertThat(result.results()).isSameAs(page);
    verify(store)
        .search(
            new SearchCriteria("kr", Optional.of(RegistrationType.STUDENT), Optional.of("b"), 2));
  }

  @Test
  void formConfigurationHasActiveOptionsConsentsAndCaptchaSettings() {
    CaptchaSettings captcha = new CaptchaSettings(true, "site");

    FormConfigurationService.FormConfiguration config =
        new FormConfigurationService(CONFIG, captcha).formConfiguration();

    assertThat(config.options()).extracting(ConferenceOption::id).containsExactly("a");
    assertThat(config.consents()).extracting(ConsentDefinition::id).containsExactly("privacy");
    assertThat(config.captcha()).isSameAs(captcha);
  }

  @Test
  void searchPageCopiesItsItems() {
    List<Registration> items = new java.util.ArrayList<>(List.of(student()));
    SearchPage page = new SearchPage(1, items);
    items.clear();

    assertThat(page.items()).hasSize(1);
    assertThat(page.total()).isEqualTo(1);
  }
}
