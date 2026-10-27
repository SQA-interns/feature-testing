package si.konferenca.registration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.application.port.CaptchaVerifier;
import si.konferenca.registration.application.port.RegistrationNotifier;
import si.konferenca.registration.domain.ConferenceCatalog;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;
import si.konferenca.registration.domain.ErrorCode;
import si.konferenca.registration.domain.FieldError;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Participant;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationRejectedException;
import si.konferenca.registration.domain.RegistrationType;
import si.konferenca.registration.domain.SelectedOption;
import si.konferenca.registration.domain.Submission;

class RegisterParticipantTest {

  private static final Instant NOW = Instant.parse("2026-10-27T10:15:30.123456789Z");
  private static final Instant NOW_MILLIS = Instant.parse("2026-10-27T10:15:30.123Z");
  private static final byte[] COPY = {1, 2, 3};

  private final ConferenceCatalog catalog =
      new ConferenceCatalog(
          List.of(
              new ConferenceOption(
                  "ws-a", "A", OptionCategory.WORKSHOP, true, Set.of(RegistrationType.EXTERNAL))),
          List.of(
              new ConsentDefinition("privacy", "Privacy", true),
              new ConsentDefinition("photos", "Photos", false)));
  private final CaptchaVerifier captcha = mock(CaptchaVerifier.class);
  private final StoreRegistration storage = mock(StoreRegistration.class);
  private final RegistrationNotifier notifier = mock(RegistrationNotifier.class);
  private final RegisterParticipant useCase =
      new RegisterParticipant(
          catalog, captcha, storage, notifier, Clock.fixed(NOW, ZoneOffset.UTC));

  private static Submission submission(String token) {
    return new Submission(
        "EXTERNAL",
        " Ana ",
        "Novak",
        "ana@example.org",
        "Institute",
        null,
        null,
        null,
        List.of("ws-a"),
        List.of("privacy"),
        token);
  }

  @Test
  void storesAndNotifiesAValidSubmission() {
    when(captcha.verify("token")).thenReturn(true);
    when(storage.store(any())).thenReturn(COPY);

    Registration registration = useCase.register(submission("token"));

    assertThat(registration.id()).isNotNull();
    assertThat(registration.type()).isEqualTo(RegistrationType.EXTERNAL);
    assertThat(registration.registeredAt()).isEqualTo(NOW_MILLIS);
    assertThat(registration.participant())
        .isEqualTo(
            new Participant("Ana", "Novak", "ana@example.org", "Institute", null, null, null));
    assertThat(registration.options())
        .containsExactly(new SelectedOption("ws-a", "A", OptionCategory.WORKSHOP));
    assertThat(registration.consents())
        .containsExactly(new GivenConsent("privacy", "Privacy", NOW_MILLIS));
    verify(storage).store(registration);
    verify(notifier).sendParticipantConfirmation(registration);
    verify(notifier).sendOrganizerNotifications(registration, COPY);
  }

  @Test
  void invalidSubmissionIsRejectedBeforeTheCaptcha() {
    Submission invalid =
        new Submission(
            "EXTERNAL",
            "",
            "Novak",
            "ana@example.org",
            "I",
            null,
            null,
            null,
            null,
            List.of("privacy"),
            "token");

    assertThatThrownBy(() -> useCase.register(invalid))
        .isInstanceOfSatisfying(
            RegistrationRejectedException.class,
            e ->
                assertThat(e.errors())
                    .containsExactly(new FieldError("firstName", ErrorCode.REQUIRED)));
    verifyNoInteractions(captcha, storage, notifier);
  }

  @Test
  void rejectedCaptchaStoresNothing() {
    when(captcha.verify("token")).thenReturn(false);

    assertThatThrownBy(() -> useCase.register(submission("token")))
        .isInstanceOfSatisfying(
            RegistrationRejectedException.class,
            e ->
                assertThat(e.errors())
                    .containsExactly(new FieldError("recaptchaToken", ErrorCode.CAPTCHA_FAILED)));
    verifyNoInteractions(storage, notifier);
  }

  @Test
  void failingEmailsDoNotChangeTheOutcome() {
    when(captcha.verify("token")).thenReturn(true);
    when(storage.store(any())).thenReturn(COPY);
    doThrow(new IllegalStateException("smtp down"))
        .when(notifier)
        .sendParticipantConfirmation(any());
    doThrow(new IllegalStateException("smtp down"))
        .when(notifier)
        .sendOrganizerNotifications(any(), any());

    Registration registration = useCase.register(submission("token"));

    verify(notifier).sendOrganizerNotifications(registration, COPY);
  }

  @Test
  void failingStorageIsNotNotified() {
    when(captcha.verify("token")).thenReturn(true);
    when(storage.store(any())).thenThrow(new IllegalStateException("disk full"));

    assertThatThrownBy(() -> useCase.register(submission("token"))).hasMessage("disk full");
    verify(notifier, never()).sendParticipantConfirmation(any());
  }
}
