package si.konferenca.registration.web;

import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.application.SearchService;
import si.konferenca.registration.domain.Registration;

/** GET /api/registrations, organizer only (api-contract.md, US-009). */
@RestController
public class SearchController {

  private final SearchService search;

  public SearchController(SearchService search) {
    this.search = search;
  }

  /** One listed registration. */
  public record SearchItem(
      String id,
      String type,
      String firstName,
      String lastName,
      String email,
      String organization,
      String studyInstitution,
      List<String> options,
      String registeredAt) {

    static SearchItem of(Registration r) {
      return new SearchItem(
          r.id().toString(),
          r.type().name(),
          r.firstName(),
          r.lastName(),
          r.email(),
          r.organization(),
          r.studyInstitution(),
          r.options(),
          DateTimeFormatter.ISO_INSTANT.format(r.registeredAt()));
    }
  }

  /** The search response: total matches, page number and at most 20 items. */
  public record SearchResponse(long total, int page, List<SearchItem> items) {}

  @GetMapping("/api/registrations")
  public ResponseEntity<Object> search(
      @RequestParam(required = false) String text,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String option,
      @RequestParam(required = false) String page) {
    SearchService.Result result = search.search(text, type, option, page);
    if (result.results() == null) {
      return ResponseEntity.badRequest().body(new ErrorResponses.FieldErrors(result.errors()));
    }
    return ResponseEntity.ok(
        new SearchResponse(
            result.results().total(),
            result.page(),
            result.results().items().stream().map(SearchItem::of).toList()));
  }
}
