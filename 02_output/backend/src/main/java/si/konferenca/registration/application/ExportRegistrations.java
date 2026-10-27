package si.konferenca.registration.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import si.konferenca.registration.application.port.RegistrationStore;
import si.konferenca.registration.application.port.RegistrationWorkbook;

/** Use case: the Excel export of every registration (US-008). */
@Service
public class ExportRegistrations {

  private final RegistrationStore registrations;
  private final RegistrationWorkbook workbook;

  public ExportRegistrations(RegistrationStore registrations, RegistrationWorkbook workbook) {
    this.registrations = registrations;
    this.workbook = workbook;
  }

  @Transactional(readOnly = true)
  public byte[] export() {
    return workbook.write(registrations.findAllOldestFirst());
  }
}
