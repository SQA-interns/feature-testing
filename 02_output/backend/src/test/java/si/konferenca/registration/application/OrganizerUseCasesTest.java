package si.konferenca.registration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.domain.ConferenceCatalogue;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.Consent;
import si.konferenca.registration.domain.FieldError;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationType;
import si.konferenca.registration.domain.SearchCriteria;
import si.konferenca.registration.domain.SearchPage;
import si.konferenca.registration.domain.SearchRequestValidator;

class OrganizerUseCasesTest {

  private static final ConferenceCatalogue CATALOGUE =
      new ConferenceCatalogue(
          List.of(
              new ConferenceOption(
                  "ws", "W", OptionCategory.WORKSHOP, true, EnumSet.allOf(RegistrationType.class)),
              new ConferenceOption(
                  "old", "O", OptionCategory.OTHER, false, EnumSet.allOf(RegistrationType.class))),
          List.of(new Consent("privacy", "I agree.", true)));

  private final RegistrationQueries queries = mock(RegistrationQueries.class);

  @Test
  void validSearchAsksTheStore() {
    SearchPage page = new SearchPage(0, 2, List.of());
    when(queries.search(any())).thenReturn(page);
    SearchRegistrations search =
        new SearchRegistrations(new SearchRequestValidator(CATALOGUE), queries);

    SearchRegistrations.Outcome outcome = search.search(" ana ", "EXTERNAL", "ws", "2");

    assertThat(outcome).isEqualTo(new SearchRegistrations.Outcome.Found(page));
    verify(queries).search(new SearchCriteria("ana", RegistrationType.EXTERNAL, "ws", 2));
  }

  @Test
  void invalidSearchIsRejectedWithoutAskingTheStore() {
    SearchRegistrations search =
        new SearchRegistrations(new SearchRequestValidator(CATALOGUE), queries);

    SearchRegistrations.Outcome outcome = search.search(null, "X", null, null);

    assertThat(((SearchRegistrations.Outcome.Rejected) outcome).errors())
        .extracting(FieldError::field)
        .containsExactly("type");
    verifyNoInteractions(queries);
  }

  @Test
  void exportWritesEveryRegistration() {
    WorkbookWriter writer = mock(WorkbookWriter.class);
    when(queries.all()).thenReturn(List.of());
    when(writer.write(List.of())).thenReturn(new byte[] {7});

    assertThat(new ExportRegistrations(queries, writer).export()).containsExactly(7);
  }

  @Test
  void formConfigurationOffersActiveOptionsAndConsents() {
    FormConfiguration form = new FormConfiguration(CATALOGUE);

    assertThat(form.activeOptions()).extracting(ConferenceOption::id).containsExactly("ws");
    assertThat(form.consents()).extracting(Consent::id).containsExactly("privacy");
  }

  @Test
  void rejectedOutcomesKeepTheirOwnErrorList() {
    List<FieldError> errors = new java.util.ArrayList<>(List.of(new FieldError("type", "bad")));
    RegistrationOutcome.Rejected rejected = new RegistrationOutcome.Rejected(errors);
    SearchRegistrations.Outcome.Rejected searchRejected =
        new SearchRegistrations.Outcome.Rejected(errors);
    errors.clear();

    assertThat(rejected.errors()).hasSize(1);
    assertThat(searchRejected.errors()).hasSize(1);
  }
}
