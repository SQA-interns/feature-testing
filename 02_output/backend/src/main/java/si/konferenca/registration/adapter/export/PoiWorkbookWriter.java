package si.konferenca.registration.adapter.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import si.konferenca.registration.application.WorkbookWriter;
import si.konferenca.registration.domain.Participant;
import si.konferenca.registration.domain.Registration;

/**
 * The export workbook of {@code api-contract.md}: first sheet, header row, one row per
 * registration; every value a string cell, never a formula (SR-07: registration data only).
 */
public class PoiWorkbookWriter implements WorkbookWriter {

  static final List<String> COLUMNS =
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

  @Override
  public byte[] write(List<Registration> registrations) {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("registrations");
      fill(sheet.createRow(0), COLUMNS);
      int index = 1;
      for (Registration registration : registrations) {
        fill(sheet.createRow(index++), values(registration));
      }
      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static List<String> values(Registration registration) {
    Participant p = registration.participant();
    return Arrays.asList(
        registration.id().toString(),
        p.type().name(),
        p.firstName(),
        p.lastName(),
        p.email(),
        p.organization(),
        p.studyInstitution(),
        p.studyProgramme(),
        p.studentId(),
        String.join(", ", registration.options()),
        String.join(", ", registration.consentIds()),
        registration.registeredAt().toString());
  }

  private static void fill(Row row, List<String> values) {
    for (int i = 0; i < values.size(); i++) {
      String value = values.get(i);
      if (value != null) {
        row.createCell(i).setCellValue(value);
      }
    }
  }
}
