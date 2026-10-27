package si.konferenca.registration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import si.konferenca.registration.application.port.JsonCopyStore;
import si.konferenca.registration.application.port.RegistrationStore;
import si.konferenca.registration.application.port.RegistrationWorkbook;
import si.konferenca.registration.domain.ConferenceCatalog;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;

class ApplicationServicesTest {

  private static final Registration REGISTRATION =
      new Registration(
          UUID.randomUUID(), RegistrationType.STUDENT, Instant.EPOCH, null, List.of(), List.of());

  private final RegistrationStore registrations = mock(RegistrationStore.class);
  private final JsonCopyStore copies = mock(JsonCopyStore.class);

  @AfterEach
  void clearSynchronization() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  private List<TransactionSynchronization> storeInTransaction() {
    TransactionSynchronizationManager.initSynchronization();
    when(copies.write(REGISTRATION)).thenReturn(new byte[] {7});

    assertThat(new StoreRegistration(registrations, copies).store(REGISTRATION)).containsExactly(7);
    InOrder order = inOrder(registrations, copies);
    order.verify(registrations).save(REGISTRATION);
    order.verify(copies).write(REGISTRATION);
    return TransactionSynchronizationManager.getSynchronizations();
  }

  @Test
  void copyIsKeptWhenTheTransactionCommits() {
    List<TransactionSynchronization> hooks = storeInTransaction();

    assertThat(hooks).hasSize(1);
    hooks.get(0).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
    verify(copies, never()).delete(REGISTRATION.id());
  }

  @Test
  void copyIsRemovedWhenTheTransactionRollsBack() {
    storeInTransaction().get(0).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

    verify(copies).delete(REGISTRATION.id());
  }

  @Test
  void copyIsRemovedWhenTheOutcomeIsUnknown() {
    storeInTransaction().get(0).afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);

    verify(copies).delete(REGISTRATION.id());
  }

  @Test
  void failedInsertWritesNoCopy() {
    TransactionSynchronizationManager.initSynchronization();
    org.mockito.Mockito.doThrow(new IllegalStateException("db"))
        .when(registrations)
        .save(REGISTRATION);

    assertThatThrownBy(() -> new StoreRegistration(registrations, copies).store(REGISTRATION))
        .hasMessage("db");
    verify(copies, never()).write(REGISTRATION);
  }

  @Test
  void formConfigurationHasActiveOptionsOnly() {
    ConferenceOption active =
        new ConferenceOption("a", "A", OptionCategory.MEAL, true, Set.of(RegistrationType.STUDENT));
    ConferenceOption inactive =
        new ConferenceOption(
            "b", "B", OptionCategory.MEAL, false, Set.of(RegistrationType.STUDENT));
    ConsentDefinition consent = new ConsentDefinition("privacy", "P", true);
    GetFormConfiguration useCase =
        new GetFormConfiguration(
            new ConferenceCatalog(List.of(active, inactive), List.of(consent)),
            new FormSettings("Conf", true, "key"));

    assertThat(useCase.get())
        .isEqualTo(new FormConfiguration("Conf", true, "key", List.of(active), List.of(consent)));
  }

  @Test
  void formConfigurationCopiesItsLists() {
    List<ConsentDefinition> consents = new ArrayList<>();
    FormConfiguration configuration = new FormConfiguration("C", false, "", List.of(), consents);
    consents.add(new ConsentDefinition("x", "X", false));

    assertThat(configuration.consents()).isEmpty();
  }

  @Test
  void exportWritesAllRegistrationsOldestFirst() {
    RegistrationWorkbook workbook = mock(RegistrationWorkbook.class);
    when(registrations.findAllOldestFirst()).thenReturn(List.of(REGISTRATION));
    when(workbook.write(List.of(REGISTRATION))).thenReturn(new byte[] {9});

    assertThat(new ExportRegistrations(registrations, workbook).export()).containsExactly(9);
  }
}
