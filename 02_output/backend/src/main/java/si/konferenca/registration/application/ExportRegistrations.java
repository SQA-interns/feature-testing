package si.konferenca.registration.application;

/** Exports every registration for organizers (US-008, BR-08, SR-07). */
public class ExportRegistrations {

  private final RegistrationRepository repository;
  private final WorkbookWriter workbookWriter;

  public ExportRegistrations(RegistrationRepository repository, WorkbookWriter workbookWriter) {
    this.repository = repository;
    this.workbookWriter = workbookWriter;
  }

  /** The workbook with one row per registration, ordered by registration time. */
  public byte[] workbook() {
    return workbookWriter.render(repository.findAllOrdered());
  }
}
