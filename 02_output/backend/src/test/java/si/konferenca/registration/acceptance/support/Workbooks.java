package si.konferenca.registration.acceptance.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** Reads the first sheet of an exported workbook ({@code GET /api/export}). */
public final class Workbooks {

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

  private Workbooks() {}

  /** The header row (first row) of the first sheet. */
  public static List<String> header(byte[] xlsx) {
    return rows(xlsx).get(0);
  }

  /** Data rows (after the header) keyed by the header names; a missing cell reads as "". */
  public static List<Map<String, String>> records(byte[] xlsx) {
    List<List<String>> rows = rows(xlsx);
    List<String> header = rows.get(0);
    List<Map<String, String>> records = new ArrayList<>();
    for (List<String> row : rows.subList(1, rows.size())) {
      Map<String, String> record = new LinkedHashMap<>();
      for (int i = 0; i < header.size(); i++) {
        record.put(header.get(i), i < row.size() ? row.get(i) : "");
      }
      records.add(record);
    }
    return records;
  }

  /** The data row whose {@code id} cell equals the id. */
  public static Map<String, String> record(byte[] xlsx, String id) {
    return records(xlsx).stream()
        .filter(r -> id.equals(r.get("id")))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No export row for " + id));
  }

  private static List<List<String>> rows(byte[] xlsx) {
    DataFormatter formatter = new DataFormatter();
    try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
      Sheet sheet = workbook.getSheetAt(0);
      List<List<String>> rows = new ArrayList<>();
      for (int r = 0; r <= sheet.getLastRowNum(); r++) {
        Row row = sheet.getRow(r);
        List<String> values = new ArrayList<>();
        if (row != null) {
          for (int c = 0; c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            values.add(cell == null ? "" : formatter.formatCellValue(cell));
          }
        }
        if (!values.stream().allMatch(String::isEmpty)) {
          rows.add(values);
        }
      }
      return rows;
    } catch (IOException e) {
      throw new AssertionError("Not a readable xlsx workbook", e);
    }
  }
}
