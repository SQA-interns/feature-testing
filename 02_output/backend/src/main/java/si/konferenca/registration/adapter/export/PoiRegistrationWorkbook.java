package si.konferenca.registration.adapter.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import si.konferenca.registration.application.port.RegistrationWorkbook;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Participant;
import si.konferenca.registration.domain.Registration;

/**
 * The export workbook of specification S4.8: one sheet, a header row, one row per registration;
 * every cell is a string cell, so no value is ever evaluated as a formula.
 */
@Component
public class PoiRegistrationWorkbook implements RegistrationWorkbook {

  static final List<String> HEADERS =
      List.of(
          "Registration ID",
          "Type",
          "Registered at",
          "First name",
          "Last name",
          "Email",
          "Organization / institution",
          "Study institution",
          "Study programme",
          "Student ID",
          "Workshops",
          "Events",
          "Meals",
          "Other activities",
          "Consents");

  @Override
  public byte[] write(List<Registration> registrations) {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Registrations");
      writeRow(sheet.createRow(0), HEADERS);
      int rowIndex = 1;
      for (Registration registration : registrations) {
        writeRow(sheet.createRow(rowIndex++), values(registration));
      }
      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException("workbook not written", e);
    }
  }

  private static void writeRow(Row row, List<String> values) {
    for (int i = 0; i < values.size(); i++) {
      row.createCell(i).setCellValue(values.get(i));
    }
  }

  private static List<String> values(Registration r) {
    Participant p = r.participant();
    List<String> values = new ArrayList<>(HEADERS.size());
    values.add(r.id().toString());
    values.add(r.type().name());
    values.add(r.registeredAt().toString());
    values.add(p.firstName());
    values.add(p.lastName());
    values.add(p.email());
    values.add(orEmpty(p.organization()));
    values.add(orEmpty(p.studyInstitution()));
    values.add(orEmpty(p.studyProgramme()));
    values.add(orEmpty(p.studentId()));
    for (OptionCategory category : OptionCategory.values()) {
      values.add(
          r.options().stream()
              .filter(o -> o.category() == category)
              .map(o -> o.name())
              .collect(Collectors.joining("; ")));
    }
    values.add(
        r.consents().stream()
            .map(c -> c.id() + " (" + c.givenAt() + ")")
            .collect(Collectors.joining("; ")));
    return values;
  }

  private static String orEmpty(String value) {
    return value == null ? "" : value;
  }
}
