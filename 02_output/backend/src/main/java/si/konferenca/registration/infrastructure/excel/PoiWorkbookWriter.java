package si.konferenca.registration.infrastructure.excel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import si.konferenca.registration.application.WorkbookWriter;

/**
 * Writes .xlsx with Apache POI. Every cell is a string cell, so text that starts with {@code =} is
 * shown, never evaluated as a formula.
 */
@Component
public class PoiWorkbookWriter implements WorkbookWriter {

  @Override
  public byte[] write(String sheetName, List<String> header, List<List<String>> rows) {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet(sheetName);
      writeRow(sheet.createRow(0), header);
      for (int i = 0; i < rows.size(); i++) {
        writeRow(sheet.createRow(i + 1), rows.get(i));
      }
      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException("Workbook could not be written", e);
    }
  }

  private static void writeRow(Row row, List<String> values) {
    for (int c = 0; c < values.size(); c++) {
      row.createCell(c).setCellValue(values.get(c));
    }
  }
}
