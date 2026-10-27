package si.konferenca.registration.application.port;

import java.util.List;
import si.konferenca.registration.domain.Registration;

/** Builds the Excel export (US-008). */
public interface RegistrationWorkbook {

  /** The workbook as .xlsx bytes, one row per registration. */
  byte[] write(List<Registration> registrations);
}
