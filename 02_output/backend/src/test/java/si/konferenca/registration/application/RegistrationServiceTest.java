package si.konferenca.registration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionSystemException;
import si.konferenca.registration.domain.ConferenceConfiguration;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;
import si.konferenca.registration.domain.FieldError;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationSubmission;
import si.konferenca.registration.domain.RegistrationType;

class RegistrationServiceTest {

  private static final Instant NOW = Instant.parse("2026-10-22T10:00:00.123Z");
  private static final byte[] COPY = {1, 2, 3};

  private final CaptchaVerifier captcha = mock(CaptchaVerifier.class);
  private final RegistrationStore store = mock(RegistrationStore.class);
  private final JsonCopyStore copies = mock(JsonCopyStore.class);
  private final Notifier notifier = mock(Notifier.class);
  private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
  private final RegistrationService service =
      new RegistrationService(
          new ConferenceConfiguration(
              List.of(
                  new ConferenceOption(
                      "ws-ai",
                      "AI",
                      OptionCategory.WORKSHOP,
                      true,
                      Set.of(RegistrationType.values()))),
              List.of(new ConsentDefinition("privacy", "I agree.", true))),
          captcha,
          store,
          copies,
          notifier,
          transactions,
          Clock.fixed(NOW, ZoneOffset.UTC));

  private static RegistrationSubmission submission(String firstName) {
    return new RegistrationSubmission(
        "EXTERNAL",
        firstName,
        "Novak",
        "ana@example.org",
        "Org",
        null,
        null,
        null,
        List.of("ws-ai"),
        List.of("privacy"),
        "token");
  }

  @Test
  void invalidSubmissionIsRejectedWithoutCaptchaOrStorage() {
    RegistrationService.Outcome outcome = service.register(submission(""));

    assertThat(outcome.accepted()).isFalse();
    assertThat(outcome.id()).isNull();
    assertThat(outcome.errors()).extracting(FieldError::field).containsExactly("firstName");
    verifyNoInteractions(captcha, store, copies, notifier, transactions);
  }

  @Test
  void rejectedCaptchaIsAFieldErrorAndNothingIsStored() {
    when(captcha.verify("token")).thenReturn(false);

    RegistrationService.Outcome outcome = service.register(submission("Ana"));

    assertThat(outcome.accepted()).isFalse();
    assertThat(outcome.errors())
        .containsExactly(new FieldError("captchaToken", "The captcha was not accepted."));
    verifyNoInteractions(store, copies, notifier, transactions);
  }

  @Test
  void unavailableCaptchaPropagates() {
    when(captcha.verify("token")).thenThrow(new CaptchaUnavailableException("down", null));

    assertThatThrownBy(() -> service.register(submission("Ana")))
        .isInstanceOf(CaptchaUnavailableException.class);
    verifyNoInteractions(store, copies);
  }

  @Test
  void acceptedRegistrationIsStoredThenCopiedThenCommittedThenNotified() {
    when(captcha.verify("token")).thenReturn(true);
    when(copies.write(any())).thenReturn(COPY);

    RegistrationService.Outcome outcome = service.register(submission(" Ana "));

    assertThat(outcome.accepted()).isTrue();
    assertThat(outcome.errors()).isEmpty();
    ArgumentCaptor<Registration> saved = ArgumentCaptor.forClass(Registration.class);
    InOrder order = inOrder(store, copies, transactions, notifier);
    order.verify(store).save(saved.capture());
    order.verify(copies).write(saved.getValue());
    order.verify(transactions).commit(any());
    order.verify(notifier).sendConfirmation(saved.getValue());
    order.verify(notifier).notifyOrganizers(saved.getValue(), COPY);
    Registration registration = saved.getValue();
    assertThat(registration.id()).isEqualTo(outcome.id());
    assertThat(registration.firstName()).isEqualTo("Ana");
    assertThat(registration.registeredAt()).isEqualTo(NOW);
  }

  @Test
  void failedJsonCopyRollsBackAndReportsStorageFailure() {
    when(captcha.verify("token")).thenReturn(true);
    StorageException failure = new StorageException("copy failed", null);
    when(copies.write(any())).thenThrow(failure);

    assertThatThrownBy(() -> service.register(submission("Ana"))).isSameAs(failure);
    verify(transactions).rollback(any());
    verify(transactions, never()).commit(any());
    verify(copies, never()).delete(any());
    verifyNoInteractions(notifier);
  }

  @Test
  void failedCommitDeletesTheJsonCopy() {
    when(captcha.verify("token")).thenReturn(true);
    when(copies.write(any())).thenReturn(COPY);
    doThrow(new TransactionSystemException("commit failed")).when(transactions).commit(any());

    assertThatThrownBy(() -> service.register(submission("Ana")))
        .isInstanceOf(StorageException.class)
        .hasMessageEndingWith("not stored")
        .hasCauseInstanceOf(TransactionSystemException.class);
    ArgumentCaptor<Registration> saved = ArgumentCaptor.forClass(Registration.class);
    verify(store).save(saved.capture());
    verify(copies).delete(saved.getValue().id());
    verifyNoInteractions(notifier);
  }

  @Test
  void failedDatabaseInsertDoesNotWriteACopy() {
    when(captcha.verify("token")).thenReturn(true);
    doThrow(new IllegalStateException("db down")).when(store).save(any());

    assertThatThrownBy(() -> service.register(submission("Ana")))
        .isInstanceOf(StorageException.class);
    verify(copies, never()).write(any());
    verify(copies, never()).delete(any());
  }

  @Test
  void failingEmailsDoNotChangeTheAcceptedRegistration() {
    when(captcha.verify("token")).thenReturn(true);
    when(copies.write(any())).thenReturn(COPY);
    doThrow(new IllegalStateException("smtp")).when(notifier).sendConfirmation(any());
    doThrow(new IllegalStateException("smtp")).when(notifier).notifyOrganizers(any(), any());

    RegistrationService.Outcome outcome = service.register(submission("Ana"));

    assertThat(outcome.accepted()).isTrue();
    verify(notifier).notifyOrganizers(any(), any());
  }
}
