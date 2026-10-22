package si.konferenca.registration.application;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import si.konferenca.registration.domain.Registration;

/**
 * The organizer export (US-008): one row per registration with exactly the columns of
 * api-contract.md, registration data only (SR-07).
 */
@Service
public class ExportService {

  public static final List<String> COLUMNS =
      List.of(
          "id",
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
          "registeredAt");

  private final RegistrationStore store;
  private final WorkbookWriter writer;

  public ExportService(RegistrationStore store, WorkbookWriter writer) {
    this.store = store;
    this.writer = writer;
  }

  public byte[] exportWorkbook() {
    List<List<String>> rows = store.findAllOldestFirst().stream().map(ExportService::row).toList();
    return writer.write("Registrations", COLUMNS, rows);
  }

  static List<String> row(Registration r) {
    return List.of(
        r.id().toString(),
        r.type().name(),
        r.firstName(),
        r.lastName(),
        r.email(),
        orEmpty(r.organization()),
        orEmpty(r.studyInstitution()),
        orEmpty(r.studyProgramme()),
        orEmpty(r.studentId()),
        String.join(", ", r.options()),
        r.consents().stream()
            .map(Registration.GivenConsent::consentId)
            .collect(Collectors.joining(", ")),
        DateTimeFormatter.ISO_INSTANT.format(r.registeredAt()));
  }

  private static String orEmpty(String value) {
    return value == null ? "" : value;
  }
}
