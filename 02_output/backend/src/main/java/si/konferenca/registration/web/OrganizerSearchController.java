package si.konferenca.registration.web;

import java.util.EnumMap;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.application.SearchCriteria;
import si.konferenca.registration.application.SearchRegistrations;
import si.konferenca.registration.domain.OptionCategory;

/**
 * The organizer search (US-009, openapi.yaml searchRegistrations); access is enforced by {@link
 * SecurityConfiguration}.
 */
@RestController
public class OrganizerSearchController {

  private final SearchRegistrations searchRegistrations;

  public OrganizerSearchController(SearchRegistrations searchRegistrations) {
    this.searchRegistrations = searchRegistrations;
  }

  @GetMapping(path = HttpsOnlyFilter.SEARCH_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<OrganizerSearchResponse> search(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String workshop,
      @RequestParam(required = false) String event,
      @RequestParam(required = false) String meal,
      @RequestParam(required = false) String other) {
    Map<OptionCategory, String> options = new EnumMap<>(OptionCategory.class);
    put(options, OptionCategory.WORKSHOP, workshop);
    put(options, OptionCategory.EVENT, event);
    put(options, OptionCategory.MEAL, meal);
    put(options, OptionCategory.OTHER, other);
    SearchCriteria criteria = new SearchCriteria(search, type, options);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(OrganizerSearchResponse.of(searchRegistrations.search(criteria)));
  }

  private static void put(Map<OptionCategory, String> options, OptionCategory key, String id) {
    if (id != null) {
      options.put(key, id);
    }
  }
}
