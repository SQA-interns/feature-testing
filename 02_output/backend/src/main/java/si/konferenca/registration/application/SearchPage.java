package si.konferenca.registration.application;

import java.util.List;
import si.konferenca.registration.domain.Registration;

/** One page of search results and the number of all matches (BR-10). */
public record SearchPage(long total, List<Registration> items) {

  public SearchPage {
    items = List.copyOf(items);
  }
}
