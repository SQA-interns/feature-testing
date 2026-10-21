package si.konferenca.registration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import si.konferenca.registration.domain.ConferenceCatalog;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;
import si.konferenca.registration.domain.FieldError;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationInput;
import si.konferenca.registration.domain.RegistrationType;
import si.konferenca.registration.domain.RegistrationValidator;

class RegisterParticipantTest {

  private static final Instant NOW = Instant.parse("2026-10-21T10:15:30.123456789Z");

  private final ConferenceCatalog catalog =
      ConferenceCatalog.of(
          List.of(
              new ConferenceOption(
                  "ws-ai", "AI", OptionCategory.WORKSHOP, true, Set.of(RegistrationType.values()))),
          List.of(new ConsentDefinition("privacy", "T", true)));

  private final List<String> calls = new ArrayList<>();
  private boolean captchaAnswer = true;
  private RuntimeException saveFailure;
  private RuntimeException copyFailure;
  private RuntimeException participantFailure;
  private RuntimeException organizerFailure;
  private byte[] organizerCopy;

  private final RegistrationRepository repository =
      new RegistrationRepository() {
        @Override
        public void save(Registration registration) {
          calls.add("save " + registration.firstName());
          if (saveFailure != null) {
            throw saveFailure;
          }
        }

        @Override
        public List<Registration> findAllOrdered() {
          return List.of();
        }
      };

  private final JsonCopyStore copies =
      new JsonCopyStore() {
        @Override
        public byte[] save(Registration registration) {
          calls.add("copy");
          if (copyFailure != null) {
            throw copyFailure;
          }
          return new byte[] {42};
        }

        @Override
        public void discard(UUID registrationId) {
          calls.add("discard");
        }
      };

  private final RegistrationNotifier notifier =
      new RegistrationNotifier() {
        @Override
        public void notifyParticipant(Registration registration) {
          calls.add("participant");
          if (participantFailure != null) {
            throw participantFailure;
          }
        }

        @Override
        public void notifyOrganizers(Registration registration, byte[] jsonCopy) {
          calls.add("organizers");
          organizerCopy = jsonCopy;
          if (organizerFailure != null) {
            throw organizerFailure;
          }
        }
      };

  private final RegisterParticipant useCase =
      new RegisterParticipant(
          new RegistrationValidator(catalog),
          token -> {
            calls.add("captcha " + token);
            return captchaAnswer;
          },
          new RegistrationStorage(repository, copies),
          notifier,
          Clock.fixed(NOW, ZoneOffset.UTC));

  @BeforeEach
  void synchronize() {
    TransactionSynchronizationManager.initSynchronization();
  }

  @AfterEach
  void clear() {
    TransactionSynchronizationManager.clearSynchronization();
  }

  private static RegistrationInput valid() {
    return new RegistrationInput(
        "EXTERNAL",
        "Ana",
        "Novak",
        "ana@example.org",
        "Org",
        null,
        null,
        null,
        List.of("ws-ai"),
        List.of("privacy"),
        "tok");
  }

  @Test
  void storesThenNotifiesInOrderAndReturnsTheRegistration() {
    Registration r = useCase.register(valid());

    assertThat(calls)
        .containsExactly("captcha tok", "save Ana", "copy", "participant", "organizers");
    assertThat(r.registeredAt()).isEqualTo(Instant.parse("2026-10-21T10:15:30.123Z"));
    assertThat(r.consents()).allSatisfy(c -> assertThat(c.givenAt()).isEqualTo(r.registeredAt()));
    assertThat(r.id()).isNotNull();
    assertThat(organizerCopy).containsExactly(42);
  }

  @Test
  void eachRegistrationGetsItsOwnId() {
    assertThat(useCase.register(valid()).id()).isNotEqualTo(useCase.register(valid()).id());
  }

  @Test
  void invalidInputIsRejectedWithoutVerifyingTheCaptchaOrStoring() {
    RegistrationInput invalid =
        new RegistrationInput(
            "EXTERNAL", "", "Novak", "x", "Org", null, null, null, null, List.of("privacy"), "t");

    assertThatThrownBy(() -> useCase.register(invalid))
        .isInstanceOfSatisfying(
            RegistrationRejectedException.class,
            e ->
                assertThat(e.errors())
                    .extracting(FieldError::field)
                    .containsExactly("firstName", "email"));
    assertThat(calls).isEmpty();
  }

  @Test
  void rejectedCaptchaIsAFieldErrorAndNothingIsStored() {
    captchaAnswer = false;

    assertThatThrownBy(() -> useCase.register(valid()))
        .isInstanceOfSatisfying(
            RegistrationRejectedException.class,
            e ->
                assertThat(e.errors())
                    .containsExactly(
                        new FieldError("captchaToken", RegisterParticipant.CAPTCHA_FAILED)));
    assertThat(calls).containsExactly("captcha tok");
  }

  @Test
  void storageFailureIsReportedAndNoEmailIsSent() {
    copyFailure = new IllegalStateException("disk full");

    assertThatThrownBy(() -> useCase.register(valid()))
        .isInstanceOf(StorageFailedException.class)
        .hasMessage("Registration could not be stored")
        .hasCause(copyFailure);
    assertThat(calls).doesNotContain("participant", "organizers");
  }

  @Test
  void databaseFailureIsReportedBeforeTheCopyIsWritten() {
    saveFailure = new IllegalStateException("db down");

    assertThatThrownBy(() -> useCase.register(valid())).isInstanceOf(StorageFailedException.class);
    assertThat(calls).containsExactly("captcha tok", "save Ana");
  }

  @Test
  void emailFailuresDoNotUndoTheRegistration() {
    participantFailure = new IllegalStateException("smtp");
    organizerFailure = new IllegalStateException("smtp");

    Registration r = useCase.register(valid());

    assertThat(r).isNotNull();
    assertThat(calls).endsWith("participant", "organizers");
  }

  @Test
  void storageRegistersACleanupThatDiscardsTheCopyUnlessCommitted() {
    useCase.register(valid());
    List<TransactionSynchronization> registered =
        TransactionSynchronizationManager.getSynchronizations();
    assertThat(registered).hasSize(1);

    registered.get(0).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
    assertThat(calls).doesNotContain("discard");
    registered.get(0).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
    assertThat(calls).endsWith("discard");
    calls.clear();
    registered.get(0).afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
    assertThat(calls).containsExactly("discard");
  }
}
