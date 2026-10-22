package si.konferenca.registration.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;

class MailTextsTest {

  private static final Instant AT = Instant.parse("2026-10-20T10:15:00.250Z");
  private static final UUID ID = UUID.fromString("00000000-0000-4000-8000-00000000000b");

  private static Registration student(List<String> options) {
    return new Registration(
        ID,
        RegistrationType.STUDENT,
        "Luka",
        "Kranjc",
        "luka@uni.si",
        null,
        "Uni",
        "CS",
        "123",
        options,
        List.of(
            new Registration.GivenConsent("privacy", AT),
            new Registration.GivenConsent("photo", AT)),
        AT);
  }

  @Test
  void confirmationListsTheOptionNames() {
    assertThat(
            MailTexts.confirmation(
                "Konf", SmtpNotifierTest.external(List.of()), List.of("AI", "Lunch")))
        .isEqualTo(
            "Dear Ana\r\nBcc: x@evil.example <b>Novák</b>,\n\n"
                + "thank you for registering for Konf.\n\n"
                + "Registration type: External participant\n"
                + "Selected options:\n- AI\n- Lunch\n\n"
                + "Registration id: 00000000-0000-4000-8000-00000000000a\n");
  }

  @Test
  void confirmationWithoutOptionsSaysNone() {
    assertThat(MailTexts.confirmation("Konf", student(List.of()), List.of()))
        .contains("Registration type: Student\n")
        .contains("Selected options:\n- none\n\nRegistration id: " + ID);
  }

  @Test
  void organizerNotificationOfStudentHasEveryField() {
    assertThat(MailTexts.organizerNotification("Konf", student(List.of("a", "b"))))
        .isEqualTo(
            "A new registration was received for Konf.\n\n"
                + "Id: "
                + ID
                + "\n"
                + "Type: STUDENT\n"
                + "First name: Luka\n"
                + "Last name: Kranjc\n"
                + "Email: luka@uni.si\n"
                + "Study institution: Uni\n"
                + "Study programme: CS\n"
                + "Student ID: 123\n"
                + "Options: a, b\n"
                + "Consents: privacy (2026-10-20T10:15:00.250Z), photo (2026-10-20T10:15:00.250Z)\n"
                + "Registered at: 2026-10-20T10:15:00.250Z\n");
  }

  @Test
  void organizerNotificationOfExternalHasOrganizationAndDashForNoOptions() {
    String text = MailTexts.organizerNotification("Konf", SmtpNotifierTest.external(List.of()));

    assertThat(text).contains("Type: EXTERNAL\n", "Organization: Org\n", "Options: -\n");
    assertThat(text).doesNotContain("Study institution");
  }
}
