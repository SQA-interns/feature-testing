package si.konferenca.registration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;
import si.konferenca.registration.domain.ConferenceConfig;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;
import si.konferenca.registration.domain.FieldError;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationSubmission;
import si.konferenca.registration.domain.RegistrationType;
import si.konferenca.registration.domain.RegistrationValidator;
import si.konferenca.registration.domain.ValidationException;

class RegistrationServiceTest {

  static final ConferenceConfig CONFIG =
      new ConferenceConfig(
          "K",
          List.of(
              new ConferenceOption(
                  "ws", "WS", OptionCategory.WORKSHOP, true, Set.of(RegistrationType.EXTERNAL))),
          List.of(new ConsentDefinition("privacy", "p", true)));

  static final Instant NOW = Instant.parse("2026-10-20T10:00:00.123456789Z");

  final List<Registration> saved = new ArrayList<>();
  final List<Registration> written = new ArrayList<>();
  final List<UUID> deleted = new ArrayList<>();
  final List<String> mails = new ArrayList<>();
  final List<String> verifiedTokens = new ArrayList<>();
  boolean captchaAccepts = true;
  RuntimeException saveFailure;
  RuntimeException writeFailure;
  RuntimeException participantMailFailure;
  RuntimeException organizerMailFailure;

  final RegistrationService service =
      new RegistrationService(
          new RegistrationValidator(CONFIG),
          token -> {
            verifiedTokens.add(token);
            return captchaAccepts;
          },
          new RegistrationRepository() {
            @Override
            public void save(Registration registration) {
              if (saveFailure != null) throw saveFailure;
              saved.add(registration);
            }

            @Override
            public List<Registration> findAll() {
              return saved;
            }
          },
          new JsonCopyStore() {
            @Override
            public byte[] write(Registration registration) {
              if (writeFailure != null) throw writeFailure;
              written.add(registration);
              return registration.id().toString().getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public void delete(UUID id) {
              deleted.add(id);
            }
          },
          new RegistrationNotifier() {
            @Override
            public void sendParticipantConfirmation(Registration registration) {
              mails.add("participant " + registration.id());
              if (participantMailFailure != null) throw participantMailFailure;
            }

            @Override
            public void sendOrganizerNotification(Registration registration, byte[] jsonCopy) {
              mails.add("organizer " + new String(jsonCopy, StandardCharsets.UTF_8));
              if (organizerMailFailure != null) throw organizerMailFailure;
            }
          },
          TransactionOperations.withoutTransaction(),
          Clock.fixed(NOW, ZoneOffset.UTC));

  static RegistrationSubmission valid() {
    return new RegistrationSubmission(
        "EXTERNAL",
        "Ana",
        "Novak",
        "a@b.org",
        "Org",
        null,
        null,
        null,
        List.of("ws"),
        List.of("privacy"),
        "token");
  }

  @Test
  void storesThenNotifiesAndReturnsTheId() {
    UUID id = service.register(valid());

    assertThat(saved).singleElement().satisfies(r -> assertThat(r.id()).isEqualTo(id));
    assertThat(written).containsExactlyElementsOf(saved);
    Registration registration = saved.get(0);
    assertThat(registration.registeredAt()).isEqualTo(Instant.parse("2026-10-20T10:00:00.123456Z"));
    assertThat(registration.consents())
        .singleElement()
        .satisfies(c -> assertThat(c.givenAt()).isEqualTo(registration.registeredAt()));
    assertThat(mails).containsExactly("participant " + id, "organizer " + id);
    assertThat(verifiedTokens).containsExactly("token");
    assertThat(deleted).isEmpty();
  }

  @Test
  void invalidSubmissionIsRejectedBeforeTheCaptchaCheck() {
    RegistrationSubmission invalid =
        new RegistrationSubmission(
            "EXTERNAL",
            "",
            "Novak",
            "a@b.org",
            "Org",
            null,
            null,
            null,
            null,
            List.of("privacy"),
            "token");

    Throwable failure = catchThrowable(() -> service.register(invalid));

    assertThat(failure).isInstanceOf(ValidationException.class);
    assertThat(verifiedTokens).isEmpty();
    assertThat(saved).isEmpty();
    assertThat(mails).isEmpty();
  }

  @Test
  void rejectedCaptchaStoresNothing() {
    captchaAccepts = false;

    Throwable failure = catchThrowable(() -> service.register(valid()));

    assertThat(((ValidationException) failure).errors())
        .containsExactly(new FieldError("captchaToken", RegistrationService.CAPTCHA_REJECTED));
    assertThat(saved).isEmpty();
    assertThat(written).isEmpty();
    assertThat(mails).isEmpty();
  }

  @Test
  void failedJsonCopyIsAStorageFailureAndCleansUp() {
    writeFailure = new IllegalStateException("disk full");

    Throwable failure = catchThrowable(() -> service.register(valid()));

    assertThat(failure).isInstanceOf(StorageException.class).hasCause(writeFailure);
    assertThat(deleted).hasSize(1);
    assertThat(mails).isEmpty();
  }

  @Test
  void failedDatabaseWriteIsAStorageFailure() {
    saveFailure = new IllegalStateException("db down");

    Throwable failure = catchThrowable(() -> service.register(valid()));

    assertThat(failure).isInstanceOf(StorageException.class).hasCause(saveFailure);
    assertThat(written).isEmpty();
    assertThat(deleted).hasSize(1);
    assertThat(mails).isEmpty();
  }

  @Test
  void mailFailuresDoNotUndoTheRegistration() {
    participantMailFailure = new IllegalStateException("smtp down");
    organizerMailFailure = new IllegalStateException("smtp down");

    UUID id = service.register(valid());

    assertThat(saved).hasSize(1);
    assertThat(mails).containsExactly("participant " + id, "organizer " + id);
    assertThat(deleted).isEmpty();
  }

  @Test
  void exportUsesAllStoredRegistrations() {
    service.register(valid());
    List<List<Registration>> exported = new ArrayList<>();
    ExportService export =
        new ExportService(
            new RegistrationRepository() {
              @Override
              public void save(Registration registration) {}

              @Override
              public List<Registration> findAll() {
                return saved;
              }
            },
            registrations -> {
              exported.add(registrations);
              return new byte[] {1, 2};
            });

    assertThat(export.exportWorkbook()).containsExactly(1, 2);
    assertThat(exported).containsExactly(saved);
  }

  @Test
  void formConfigServiceDefaultsTheSiteKey() {
    FormConfigService none = new FormConfigService(CONFIG, null, true);
    assertThat(none.captchaSiteKey()).isEmpty();
    assertThat(none.captchaTestMode()).isTrue();
    assertThat(none.conference()).isSameAs(CONFIG);
    assertThat(new FormConfigService(CONFIG, "site", false).captchaSiteKey()).isEqualTo("site");
  }
}
