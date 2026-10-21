package si.konferenca.registration.application;

import java.util.List;
import si.konferenca.registration.domain.Registration;

/** Port: renders registrations as an Excel workbook ({@code openapi.json} export). */
public interface WorkbookWriter {

  byte[] render(List<Registration> registrations);
}
