package si.konferenca.registration.infrastructure.excel;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class PoiWorkbookWriterTest {

  @Test
  void writesHeaderAndRowsAsTextCellsOnTheNamedSheet() throws Exception {
    byte[] bytes =
        new PoiWorkbookWriter()
            .write(
                "Registrations",
                List.of("id", "name"),
                List.of(List.of("1", "=1+1"), List.of("2", "Čšž")));

    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
      Sheet sheet = workbook.getSheetAt(0);
      assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
      assertThat(sheet.getSheetName()).isEqualTo("Registrations");
      assertThat(sheet.getLastRowNum()).isEqualTo(2);
      assertThat(sheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("name");
      assertThat(sheet.getRow(1).getCell(1).getCellType()).isEqualTo(CellType.STRING);
      assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("=1+1");
      assertThat(sheet.getRow(2).getCell(0).getStringCellValue()).isEqualTo("2");
      assertThat(sheet.getRow(2).getCell(1).getStringCellValue()).isEqualTo("Čšž");
    }
  }

  @Test
  void emptyTableHasOnlyTheHeader() throws Exception {
    byte[] bytes = new PoiWorkbookWriter().write("S", List.of("a"), List.of());

    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
      assertThat(workbook.getSheetAt(0).getLastRowNum()).isZero();
    }
  }
}
