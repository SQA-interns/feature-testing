package si.konferenca.registration.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import si.konferenca.registration.application.SearchResult;
import si.konferenca.registration.domain.Participant;
import si.konferenca.registration.domain.Registration;

/**
 * SearchResult of openapi.yaml: each row holds only the data of the registration's export row
 * (BR-10, SR-07).
 */
record OrganizerSearchResponse(
    int count, List<Row> registrations, List<CategoryCounts> optionCounts) {

  OrganizerSearchResponse {
    registrations = List.copyOf(registrations);
    optionCounts = List.copyOf(optionCounts);
  }

  static OrganizerSearchResponse of(SearchResult result) {
    return new OrganizerSearchResponse(
        result.registrations().size(),
        result.registrations().stream().map(Row::of).toList(),
        result.optionCounts().stream().map(CategoryCounts::of).toList());
  }

  /** SearchRegistration; the fields of the other registration type are left out. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  record Row(
      String id,
      String type,
      String registeredAt,
      String firstName,
      String lastName,
      String email,
      String organization,
      String studyInstitution,
      String studyProgramme,
      String studentId,
      List<ApiResponses.Option> options,
      List<Consent> consents) {

    Row {
      options = List.copyOf(options);
      consents = List.copyOf(consents);
    }

    static Row of(Registration r) {
      Participant p = r.participant();
      return new Row(
          r.id().toString(),
          r.type().name(),
          r.registeredAt().toString(),
          p.firstName(),
          p.lastName(),
          p.email(),
          p.organization(),
          p.studyInstitution(),
          p.studyProgramme(),
          p.studentId(),
          r.options().stream()
              .map(o -> new ApiResponses.Option(o.id(), o.name(), o.category().name()))
              .toList(),
          r.consents().stream().map(c -> new Consent(c.id(), c.givenAt().toString())).toList());
    }
  }

  /** A given consent: its id and when it was given. */
  record Consent(String id, String givenAt) {}

  /** CategoryCounts. */
  record CategoryCounts(String category, List<OptionCount> options) {

    CategoryCounts {
      options = List.copyOf(options);
    }

    static CategoryCounts of(SearchResult.CategoryCount c) {
      return new CategoryCounts(
          c.category().name(),
          c.options().stream()
              .map(
                  o ->
                      new OptionCount(
                          o.option().id(), o.option().name(), o.option().active(), o.count()))
              .toList());
    }
  }

  /** The count of one configured option. */
  record OptionCount(String id, String name, boolean active, int count) {}
}
