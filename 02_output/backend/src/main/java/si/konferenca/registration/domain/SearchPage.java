package si.konferenca.registration.domain;

import java.util.List;

/** One page of search results with the total number of matches. */
public record SearchPage(long total, int page, List<Registration> items) {

  public SearchPage {
    items = List.copyOf(items);
  }
}
