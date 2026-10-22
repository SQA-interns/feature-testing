package si.konferenca.registration.adapter.web;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.application.SearchRegistrations;
import si.konferenca.registration.domain.Participant;
import si.konferenca.registration.domain.Registration;

/** {@code GET /api/registrations} (organizer): search and list registrations (US-009). */
@RestController
public class SearchController {

  record Item(
      String id,
      String type,
      String firstName,
      String lastName,
      String email,
      String organization,
      String studyInstitution,
      List<String> options,
      String registeredAt) {}

  record SearchResponse(long total, int page, List<Item> items) {}

  private final SearchRegistrations searchRegistrations;

  public SearchController(SearchRegistrations searchRegistrations) {
    this.searchRegistrations = searchRegistrations;
  }

  @GetMapping(path = "/api/registrations", produces = ErrorResponses.JSON_UTF8)
  ResponseEntity<Object> search(
      @RequestParam(required = false) String text,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String option,
      @RequestParam(required = false) String page) {
    return switch (searchRegistrations.search(text, type, option, page)) {
      case SearchRegistrations.Outcome.Found found ->
          ResponseEntity.ok(
              new SearchResponse(
                  found.page().total(),
                  found.page().page(),
                  found.page().items().stream().map(SearchController::item).toList()));
      case SearchRegistrations.Outcome.Rejected rejected ->
          ResponseEntity.badRequest()
              .body(
                  new ErrorBody(
                      rejected.errors().stream()
                          .map(e -> new ErrorBody.Item(e.field(), e.message()))
                          .toList()));
    };
  }

  private static Item item(Registration registration) {
    Participant p = registration.participant();
    return new Item(
        registration.id().toString(),
        p.type().name(),
        p.firstName(),
        p.lastName(),
        p.email(),
        p.organization(),
        p.studyInstitution(),
        registration.options(),
        registration.registeredAt().toString());
  }
}
