package si.konferenca.registration.domain;

import java.util.List;

/** One page of a search: the registrations, newest first, and the number of all matches. */
public record RegistrationPage(long total, int page, List<Registration> items) {

  public RegistrationPage {
    items = List.copyOf(items);
  }
}
