package si.konferenca.registration.adapter.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Participant;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;
import si.konferenca.registration.domain.SelectedOption;

class PoiRegistrationWorkbookTest {

  private static final Instant AT = Instant.parse("2026-10-27T10:00:00.120Z");

  private static List<List<String>> rows(byte[] xlsx) throws IOException {
    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
      assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
      Sheet sheet = workbook.getSheetAt(0);
      assertThat(sheet.getSheetName()).isEqualTo("Registrations");
      List<List<String>> rows = new ArrayList<>();
      for (Row row : sheet) {
        List<String> values = new ArrayList<>();
        for (Cell cell : row) {
          assertThat(cell.getCellType()).isEqualTo(CellType.STRING);
          values.add(cell.getStringCellValue());
        }
        rows.add(values);
      }
      return rows;
    }
  }

  @Test
  void emptyExportHasTheHeaderRowOnly() throws IOException {
    assertThat(rows(new PoiRegistrationWorkbook().write(List.of())))
        .containsExactly(PoiRegistrationWorkbook.HEADERS);
  }

  @Test
  void oneRowPerRegistrationWithEveryValueAsText() throws IOException {
    UUID first = UUID.fromString("00000000-0000-0000-0000-000000000001");
    UUID second = UUID.fromString("00000000-0000-0000-0000-000000000002");
    Registration external =
        new Registration(
            first,
            RegistrationType.EXTERNAL,
            AT,
            new Participant("=1+1", "Novak", "a@x.si", "Inštitut", null, null, null),
            List.of(
                new SelectedOption("w1", "Testing", OptionCategory.WORKSHOP),
                new SelectedOption("e", "Dinner", OptionCategory.EVENT),
                new SelectedOption("w2", "Security", OptionCategory.WORKSHOP),
                new SelectedOption("m", "Lunch", OptionCategory.MEAL),
                new SelectedOption("o", "Tour", OptionCategory.OTHER)),
            List.of(new GivenConsent("privacy", "P", AT), new GivenConsent("photos", "F", AT)));
    Registration student =
        new Registration(
            second,
            RegistrationType.STUDENT,
            AT,
            new Participant("Žiga", "Kos", "z@x.si", null, "UL", "FRI", "0123"),
            List.of(),
            List.of());

    List<List<String>> rows = rows(new PoiRegistrationWorkbook().write(List.of(external, student)));

    assertThat(rows).hasSize(3);
    assertThat(rows.get(1))
        .containsExactly(
            first.toString(),
            "EXTERNAL",
            "2026-10-27T10:00:00.120Z",
            "=1+1",
            "Novak",
            "a@x.si",
            "Inštitut",
            "",
            "",
            "",
            "Testing; Security",
            "Dinner",
            "Lunch",
            "Tour",
            "privacy (2026-10-27T10:00:00.120Z); photos (2026-10-27T10:00:00.120Z)");
    assertThat(rows.get(2))
        .containsExactly(
            second.toString(),
            "STUDENT",
            "2026-10-27T10:00:00.120Z",
            "Žiga",
            "Kos",
            "z@x.si",
            "",
            "UL",
            "FRI",
            "0123",
            "",
            "",
            "",
            "",
            "");
  }
}
