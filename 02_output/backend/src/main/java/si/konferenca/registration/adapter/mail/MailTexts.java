package si.konferenca.registration.adapter.mail;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Participant;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;
import si.konferenca.registration.domain.SelectedOption;

/** Plain-text bodies of the two emails (emails.md); user values never span lines (SR-05). */
final class MailTexts {

  private static final Map<OptionCategory, String> CATEGORY_LABELS =
      Map.of(
          OptionCategory.WORKSHOP, "Workshops",
          OptionCategory.EVENT, "Events",
          OptionCategory.MEAL, "Meals",
          OptionCategory.OTHER, "Other activities");

  private MailTexts() {}

  static String participant(Registration r, String conferenceName) {
    Participant p = r.participant();
    StringBuilder text = new StringBuilder(512);
    text.append(conferenceName)
        .append("\n\nDear ")
        .append(line(p.firstName()))
        .append(' ')
        .append(line(p.lastName()))
        .append(",\n\nyour registration has been received.\n\nRegistration id: ")
        .append(r.id())
        .append("\nRegistration type: ")
        .append(typeLabel(r.type()))
        .append("\n\nSelected options:\n");
    appendOptions(text, r.options());
    return text.toString();
  }

  static String organizer(Registration r, String conferenceName) {
    Participant p = r.participant();
    StringBuilder text = new StringBuilder(1024);
    text.append("New registration for ")
        .append(conferenceName)
        .append("\n\nRegistration id: ")
        .append(r.id())
        .append("\nType: ")
        .append(typeLabel(r.type()))
        .append("\nRegistered at: ")
        .append(r.registeredAt())
        .append('\n');
    field(text, "First name", p.firstName());
    field(text, "Last name", p.lastName());
    field(text, "Email", p.email());
    field(text, "Organization / institution", p.organization());
    field(text, "Study institution", p.studyInstitution());
    field(text, "Study programme", p.studyProgramme());
    field(text, "Student ID", p.studentId());
    text.append("\nSelected options:\n");
    appendOptions(text, r.options());
    text.append("\nConsents:\n");
    r.consents()
        .forEach(
            c ->
                text.append("- ")
                    .append(line(c.text()))
                    .append(" (")
                    .append(c.id())
                    .append(", given ")
                    .append(c.givenAt())
                    .append(")\n"));
    return text.toString();
  }

  private static void field(StringBuilder text, String label, String value) {
    if (value != null) {
      text.append(label).append(": ").append(line(value)).append('\n');
    }
  }

  private static void appendOptions(StringBuilder text, List<SelectedOption> options) {
    if (options.isEmpty()) {
      text.append("No options selected.\n");
      return;
    }
    for (OptionCategory category : OptionCategory.values()) {
      String names =
          options.stream()
              .filter(o -> o.category() == category)
              .map(o -> line(o.name()))
              .collect(Collectors.joining("; "));
      if (!names.isEmpty()) {
        text.append(CATEGORY_LABELS.get(category)).append(": ").append(names).append('\n');
      }
    }
  }

  private static String typeLabel(RegistrationType type) {
    return type == RegistrationType.EXTERNAL ? "External participant" : "Student";
  }

  /** Replaces CR and LF so a value stays on its own line. */
  static String line(String value) {
    return value.replace('\r', ' ').replace('\n', ' ');
  }
}
