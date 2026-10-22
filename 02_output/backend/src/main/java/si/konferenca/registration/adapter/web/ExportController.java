package si.konferenca.registration.adapter.web;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.application.ExportRegistrations;

/** {@code GET /api/export} (organizer): every registration as an Excel workbook (US-008). */
@RestController
public class ExportController {

  static final MediaType XLSX =
      MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  private final ExportRegistrations exportRegistrations;

  public ExportController(ExportRegistrations exportRegistrations) {
    this.exportRegistrations = exportRegistrations;
  }

  @GetMapping("/api/export")
  ResponseEntity<byte[]> export() {
    return ResponseEntity.ok()
        .contentType(XLSX)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename("registrations.xlsx").build().toString())
        .body(exportRegistrations.export());
  }
}
