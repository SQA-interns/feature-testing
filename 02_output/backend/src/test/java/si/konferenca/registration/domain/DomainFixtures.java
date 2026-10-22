package si.konferenca.registration.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Shared configuration and submissions for the domain unit tests. */
final class DomainFixtures {

  private DomainFixtures() {}

  static ConferenceOption option(String id, OptionCategory category, RegistrationType... types) {
    Set<RegistrationType> availableTo =
        types.length == 0 ? EnumSet.allOf(RegistrationType.class) : Set.of(types);
    return new ConferenceOption(id, "Name of " + id, category, true, availableTo);
  }

  static ConferenceConfiguration configuration() {
    return new ConferenceConfiguration(
        List.of(
            option("ws-ai", OptionCategory.WORKSHOP),
            option("ws-cloud", OptionCategory.WORKSHOP, RegistrationType.EXTERNAL),
            option("ev-night", OptionCategory.EVENT, RegistrationType.STUDENT),
            new ConferenceOption(
                "meal-old",
                "Old meal",
                OptionCategory.MEAL,
                false,
                Set.of(RegistrationType.values()))),
        List.of(
            new ConsentDefinition("privacy", "I agree.", true),
            new ConsentDefinition("photo", "Photos.", false)));
  }

  static RegistrationSubmission external() {
    return new RegistrationSubmission(
        "EXTERNAL",
        "Ana",
        "Novak",
        "ana@example.org",
        "Example d.o.o.",
        null,
        null,
        null,
        List.of("ws-ai"),
        List.of("privacy"),
        "token");
  }

  static RegistrationSubmission student() {
    return new RegistrationSubmission(
        "STUDENT",
        "Luka",
        "Kranjc",
        "luka@uni-lj.si",
        null,
        "University of Ljubljana",
        "Computer Science",
        "63200001",
        List.of("ev-night"),
        List.of("privacy"),
        "token");
  }

  /** A copy of the submission with one field replaced (by record component name). */
  static RegistrationSubmission with(RegistrationSubmission s, String field, Object value) {
    List<Object> values =
        new ArrayList<>(
            Arrays.asList(
                s.type(),
                s.firstName(),
                s.lastName(),
                s.email(),
                s.organization(),
                s.studyInstitution(),
                s.studyProgramme(),
                s.studentId(),
                s.options(),
                s.consents(),
                s.captchaToken()));
    List<String> names =
        List.of(
            "type",
            "firstName",
            "lastName",
            "email",
            "organization",
            "studyInstitution",
            "studyProgramme",
            "studentId",
            "options",
            "consents",
            "captchaToken");
    values.set(names.indexOf(field), value);
    @SuppressWarnings("unchecked")
    RegistrationSubmission copy =
        new RegistrationSubmission(
            (String) values.get(0),
            (String) values.get(1),
            (String) values.get(2),
            (String) values.get(3),
            (String) values.get(4),
            (String) values.get(5),
            (String) values.get(6),
            (String) values.get(7),
            (List<String>) values.get(8),
            (List<String>) values.get(9),
            (String) values.get(10));
    return copy;
  }
}
