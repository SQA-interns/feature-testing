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
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import si.konferenca.registration.domain.ConferenceCatalogue;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.Consent;
import si.konferenca.registration.domain.FieldError;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationForm;
import si.konferenca.registration.domain.RegistrationType;
import si.konferenca.registration.domain.RegistrationValidator;

class RegisterParticipantTest {

  private static final Instant NOW = Instant.parse("2026-10-20T10:15:30.123456789Z");

  private final CaptchaVerifier captcha = mock(CaptchaVerifier.class);
  private final RegistrationStore store = mock(RegistrationStore.class);
  private final JsonCopyStore copies = mock(JsonCopyStore.class);
  private final Notifier notifier = mock(Notifier.class);
  private final TransactionRunner transactions = Runnable::run;
  private final RegisterParticipant useCase =
      new RegisterParticipant(
          new RegistrationValidator(
              new ConferenceCatalogue(
                  List.of(
                      new ConferenceOption(
                          "ws",
                          "W",
                          OptionCategory.WORKSHOP,
                          true,
                          EnumSet.allOf(RegistrationType.class))),
                  List.of(new Consent("privacy", "I agree.", true)))),
          captcha,
          store,
          copies,
          transactions,
          notifier,
          Clock.fixed(NOW, ZoneOffset.UTC));

  private static RegistrationForm form(String token) {
    return new RegistrationForm(
        "EXTERNAL",
        "Ana",
        "Novak",
        "ana@example.org",
        "Org",
        null,
        null,
        null,
        List.of("ws"),
        List.of("privacy"),
        token);
  }

  @Test
  void acceptedRegistrationIsStoredCopiedAndNotified() {
    when(captcha.verify("tok")).thenReturn(true);
    when(copies.write(any())).thenReturn(new byte[] {1, 2});

    RegistrationOutcome outcome = useCase.register(form(" tok "));

    ArgumentCaptor<Registration> stored = ArgumentCaptor.forClass(Registration.class);
    verify(store).add(stored.capture());
    Registration registration = stored.getValue();
    assertThat(outcome).isEqualTo(new RegistrationOutcome.Accepted(registration.id()));
    assertThat(registration.registeredAt()).isEqualTo(Instant.parse("2026-10-20T10:15:30.123456Z"));
    assertThat(registration.consents())
        .containsExactly(new GivenConsent("privacy", registration.registeredAt()));
    assertThat(registration.options()).containsExactly("ws");
    assertThat(registration.participant().lastName()).isEqualTo("Novak");
    verify(copies).write(registration);
    verify(notifier).registrationAccepted(registration, new byte[] {1, 2});
    verify(copies, never()).delete(any());
  }

  @Test
  void missingOrBlankTokenIsRejectedWithoutAskingTheVerifier() {
    RegistrationOutcome missing = useCase.register(form(null));
    RegistrationOutcome blank = useCase.register(form("   "));

    assertThat(missing).isEqualTo(blank);
    assertThat(((RegistrationOutcome.Rejected) missing).errors())
        .extracting(FieldError::field)
        .containsExactly("captchaToken");
    verifyNoInteractions(captcha, store, copies, notifier);
  }

  @Test
  void tokenTheVerifierRejectsIsRejected() {
    when(captcha.verify("bad")).thenReturn(false);

    RegistrationOutcome outcome = useCase.register(form("bad"));

    assertThat(((RegistrationOutcome.Rejected) outcome).errors().getFirst().message())
        .contains("reCAPTCHA");
    verifyNoInteractions(store, copies, notifier);
  }

  @Test
  void fieldErrorsAndCaptchaErrorsAreReportedTogether() {
    when(captcha.verify("bad")).thenReturn(false);
    RegistrationForm invalid =
        new RegistrationForm(
            "EXTERNAL",
            "",
            "Novak",
            "ana@example.org",
            "Org",
            null,
            null,
            null,
            List.of(),
            List.of("privacy"),
            "bad");

    RegistrationOutcome outcome = useCase.register(invalid);

    assertThat(((RegistrationOutcome.Rejected) outcome).errors())
        .extracting(FieldError::field)
        .containsExactly("firstName", "captchaToken");
    verify(captcha).verify("bad");
    verifyNoInteractions(store);
  }

  @Test
  void failedCopyRemovesItAndReportsNotStored() {
    when(captcha.verify("tok")).thenReturn(true);
    when(copies.write(any())).thenThrow(new IllegalStateException("disk full"));

    assertThatThrownBy(() -> useCase.register(form("tok")))
        .isInstanceOf(RegistrationNotStoredException.class)
        .hasMessageContaining("could not be stored")
        .hasCauseInstanceOf(IllegalStateException.class);
    verify(copies).delete(any());
    verifyNoInteractions(notifier);
  }

  @Test
  void failedStoreAlsoReportsNotStored() {
    when(captcha.verify("tok")).thenReturn(true);
    doThrow(new IllegalStateException("db down")).when(store).add(any());

    assertThatThrownBy(() -> useCase.register(form("tok")))
        .isInstanceOf(RegistrationNotStoredException.class);
    verify(copies, never()).write(any());
    verify(copies).delete(any());
  }

  @Test
  void notificationFailureKeepsTheRegistrationAccepted() {
    when(captcha.verify("tok")).thenReturn(true);
    doThrow(new IllegalStateException("smtp down"))
        .when(notifier)
        .registrationAccepted(any(), any());

    assertThat(useCase.register(form("tok"))).isInstanceOf(RegistrationOutcome.Accepted.class);
    verify(copies, never()).delete(any());
  }
}
