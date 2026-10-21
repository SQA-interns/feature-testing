package si.konferenca.registration.adapter.web;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.application.SearchService;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationPage;

/**
 * {@code GET /api/registrations}: the organizer's search (US-009); access in SecurityConfig. Only
 * the fields of api-contract.md leave the backend (SR-07).
 */
@RestController
class SearchController {

  private final SearchService search;

  SearchController(SearchService search) {
    this.search = search;
  }

  @GetMapping(value = "/api/registrations", produces = MediaType.APPLICATION_JSON_VALUE)
  SearchResponse search(
      @RequestParam(required = false) String text,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String option,
      @RequestParam(required = false) String page) {
    RegistrationPage result = search.search(text, type, option, page);
    return new SearchResponse(
        result.total(), result.page(), result.items().stream().map(ItemResponse::of).toList());
  }

  record SearchResponse(long total, int page, List<ItemResponse> items) {}

  record ItemResponse(
      String id,
      String type,
      String firstName,
      String lastName,
      String email,
      String organization,
      String studyInstitution,
      List<String> options,
      String registeredAt) {

    static ItemResponse of(Registration registration) {
      return new ItemResponse(
          registration.id().toString(),
          registration.type().name(),
          registration.firstName(),
          registration.lastName(),
          registration.email(),
          registration.organization(),
          registration.studyInstitution(),
          registration.options().stream().sorted().toList(),
          registration.registeredAt().toString());
    }
  }
}
