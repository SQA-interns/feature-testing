package si.konferenca.registration.adapter.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.Participant;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;

class PoiWorkbookWriterTest {

  private static final Instant AT = Instant.parse("2026-10-20T10:15:30Z");

  @Test
  void writesHeaderAndOneStringRowPerRegistration() throws IOException {
    Registration external =
        new Registration(
            UUID.fromString("00000000-0000-4000-8000-000000000001"),
            new Participant(
                RegistrationType.EXTERNAL, "=1+1", "Novak", "a@b.si", "Org", null, null, null),
            List.of("ws", "ev"),
            List.of(new GivenConsent("privacy", AT), new GivenConsent("photos", AT)),
            AT);
    Registration student =
        new Registration(
            UUID.fromString("00000000-0000-4000-8000-000000000002"),
            new Participant(
                RegistrationType.STUDENT, "Luka", "Šef", "l@u.si", null, "Uni", "CS", "63"),
            List.of(),
            List.of(new GivenConsent("privacy", AT)),
            AT);

    byte[] bytes = new PoiWorkbookWriter().write(List.of(external, student));

    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
      Sheet sheet = workbook.getSheetAt(0);
      assertThat(sheet.getSheetName()).isEqualTo("registrations");
      assertThat(sheet.getLastRowNum()).isEqualTo(2);
      assertThat(values(sheet.getRow(0))).isEqualTo(PoiWorkbookWriter.COLUMNS);
      Row first = sheet.getRow(1);
      assertThat(values(first))
          .containsExactly(
              "00000000-0000-4000-8000-000000000001",
              "EXTERNAL",
              "=1+1",
              "Novak",
              "a@b.si",
              "Org",
              "",
              "",
              "",
              "ws, ev",
              "privacy, photos",
              "2026-10-20T10:15:30Z");
      assertThat(first.getCell(2).getCellType()).as("never a formula").isEqualTo(CellType.STRING);
      assertThat(first.getCell(6)).isNull();
      assertThat(values(sheet.getRow(2)))
          .containsExactly(
              "00000000-0000-4000-8000-000000000002",
              "STUDENT",
              "Luka",
              "Šef",
              "l@u.si",
              "",
              "Uni",
              "CS",
              "63",
              "",
              "privacy",
              "2026-10-20T10:15:30Z");
    }
  }

  @Test
  void emptyExportHasOnlyTheHeader() throws IOException {
    try (XSSFWorkbook workbook =
        new XSSFWorkbook(new ByteArrayInputStream(new PoiWorkbookWriter().write(List.of())))) {
      assertThat(workbook.getSheetAt(0).getLastRowNum()).isZero();
      assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
    }
  }

  private static List<String> values(Row row) {
    return java.util.stream.IntStream.range(0, PoiWorkbookWriter.COLUMNS.size())
        .mapToObj(i -> row.getCell(i) == null ? "" : row.getCell(i).getStringCellValue())
        .toList();
  }
}
