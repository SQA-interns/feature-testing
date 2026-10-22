package si.konferenca.registration.application;

import java.util.List;

/** Port: writes a table as an Excel workbook (.xlsx) whose cells are all text. */
public interface WorkbookWriter {

  byte[] write(String sheetName, List<String> header, List<List<String>> rows);
}
