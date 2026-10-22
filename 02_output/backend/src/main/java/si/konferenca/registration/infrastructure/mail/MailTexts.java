package si.konferenca.registration.infrastructure.mail;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;

/** Plain-text email bodies (02_contracts/email-contract.md); input stays literal text (SR-05). */
final class MailTexts {

  private MailTexts() {}

  static String confirmation(String conference, Registration r, List<String> optionNames) {
    StringBuilder text = new StringBuilder();
    text.append("Dear ").append(r.firstName()).append(' ').append(r.lastName()).append(",\n\n");
    text.append("thank you for registering for ").append(conference).append(".\n\n");
    text.append("Registration type: ")
        .append(r.type() == RegistrationType.EXTERNAL ? "External participant" : "Student")
        .append('\n');
    text.append("Selected options:\n");
    if (optionNames.isEmpty()) {
      text.append("- none\n");
    }
    optionNames.forEach(name -> text.append("- ").append(name).append('\n'));
    text.append("\nRegistration id: ").append(r.id()).append('\n');
    return text.toString();
  }

  static String organizerNotification(String conference, Registration r) {
    StringBuilder text = new StringBuilder();
    text.append("A new registration was received for ").append(conference).append(".\n\n");
    line(text, "Id", r.id().toString());
    line(text, "Type", r.type().name());
    line(text, "First name", r.firstName());
    line(text, "Last name", r.lastName());
    line(text, "Email", r.email());
    if (r.type() == RegistrationType.EXTERNAL) {
      line(text, "Organization", r.organization());
    } else {
      line(text, "Study institution", r.studyInstitution());
      line(text, "Study programme", r.studyProgramme());
      line(text, "Student ID", r.studentId());
    }
    line(text, "Options", r.options().isEmpty() ? "-" : String.join(", ", r.options()));
    line(
        text,
        "Consents",
        r.consents().stream()
            .map(
                c -> c.consentId() + " (" + DateTimeFormatter.ISO_INSTANT.format(c.givenAt()) + ")")
            .collect(Collectors.joining(", ")));
    line(text, "Registered at", DateTimeFormatter.ISO_INSTANT.format(r.registeredAt()));
    return text.toString();
  }

  private static void line(StringBuilder text, String label, String value) {
    text.append(label).append(": ").append(value).append('\n');
  }
}
