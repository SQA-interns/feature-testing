package si.konferenca.registration.adapter.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import si.konferenca.registration.application.RegistrationExporter;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.Registration;

/**
 * The organizer workbook (US-008, api-contract.md): first sheet, header row, one row per
 * registration. Every cell is a string cell, so no value is ever read as a formula (SB-05); only
 * registration fields are exported (SR-07).
 */
public class ExcelRegistrationExporter implements RegistrationExporter {

  static final List<String> HEADERS =
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

  static final String SHEET = "registrations";

  @Override
  public byte[] export(List<Registration> registrations) {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet(SHEET);
      write(sheet.createRow(0), HEADERS);
      int rowIndex = 1;
      for (Registration registration : registrations) {
        write(sheet.createRow(rowIndex++), values(registration));
      }
      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException("workbook could not be written", e);
    }
  }

  static List<String> values(Registration r) {
    return List.of(
        r.id().toString(),
        r.type().name(),
        r.firstName(),
        r.lastName(),
        r.email(),
        empty(r.organization()),
        empty(r.studyInstitution()),
        empty(r.studyProgramme()),
        empty(r.studentId()),
        String.join(", ", r.options()),
        String.join(", ", r.consents().stream().map(GivenConsent::id).toList()),
        DateTimeFormatter.ISO_INSTANT.format(r.registeredAt()));
  }

  private static void write(Row row, List<String> values) {
    for (int column = 0; column < values.size(); column++) {
      row.createCell(column).setCellValue(values.get(column));
    }
  }

  private static String empty(String value) {
    return value == null ? "" : value;
  }
}
