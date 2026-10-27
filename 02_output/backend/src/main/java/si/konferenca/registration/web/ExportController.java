package si.konferenca.registration.web;

import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.application.ExportRegistrations;

/** The organizer export (US-008); access is enforced by {@link SecurityConfiguration}. */
@RestController
public class ExportController {

  static final MediaType XLSX =
      MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  private final ExportRegistrations exportRegistrations;

  public ExportController(ExportRegistrations exportRegistrations) {
    this.exportRegistrations = exportRegistrations;
  }

  @GetMapping(HttpsOnlyFilter.EXPORT_PATH)
  ResponseEntity<byte[]> export() {
    String fileName = "registrations-" + LocalDate.now(ZoneOffset.UTC) + ".xlsx";
    return ResponseEntity.ok()
        .contentType(XLSX)
        .cacheControl(CacheControl.noStore())
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(fileName).build().toString())
        .body(exportRegistrations.export());
  }
}
