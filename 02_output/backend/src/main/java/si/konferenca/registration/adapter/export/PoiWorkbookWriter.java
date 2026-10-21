package si.konferenca.registration.adapter.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import si.konferenca.registration.application.WorkbookWriter;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.Registration;

/**
 * Writes the export workbook: header row of the contract columns, one row per registration, every
 * value a string cell so user text is never evaluated as a formula (SR-07, AC-008-01).
 */
public class PoiWorkbookWriter implements WorkbookWriter {

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

  @Override
  public byte[] render(List<Registration> registrations) {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Registrations");
      write(sheet.createRow(0), COLUMNS);
      int index = 1;
      for (Registration registration : registrations) {
        write(sheet.createRow(index++), values(registration));
      }
      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException("Workbook could not be written", e);
    }
  }

  private static List<String> values(Registration r) {
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
        String.join(", ", r.consents().stream().map(GivenConsent::id).toList()),
        r.registeredAt().toString());
  }

  private static void write(Row row, List<String> values) {
    for (int i = 0; i < values.size(); i++) {
      row.createCell(i).setCellValue(values.get(i));
    }
  }

  private static String orEmpty(String value) {
    return value == null ? "" : value;
  }
}
