package si.konferenca.registration.domain;

import java.util.List;
import java.util.Optional;

/**
 * Which registrations the organizer search lists (BR-11, BR-12): the search text, the registration
 * type and one option id per filtered category must all match.
 */
public record RegistrationFilter(
    SearchText text, Optional<RegistrationType> type, List<String> optionIds) {

  /** Copies the option ids so the filter cannot change afterwards. */
  public RegistrationFilter {
    optionIds = List.copyOf(optionIds);
  }

  public boolean matches(Registration registration) {
    if (type.isPresent() && type.get() != registration.type()) {
      return false;
    }
    for (String optionId : optionIds) {
      if (registration.options().stream().noneMatch(o -> o.id().equals(optionId))) {
        return false;
      }
    }
    Participant p = registration.participant();
    return text.matchesAny(
        p.firstName(), p.lastName(), p.email(), p.organization(), p.studyInstitution());
  }
}
