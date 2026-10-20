package si.konferenca.registration.application;

/** Use case: the organizer's Excel export of every registration (US-008). */
public class ExportService {

  private final RegistrationRepository repository;
  private final RegistrationExporter exporter;

  public ExportService(RegistrationRepository repository, RegistrationExporter exporter) {
    this.repository = repository;
    this.exporter = exporter;
  }

  public byte[] exportWorkbook() {
    return exporter.export(repository.findAll());
  }
}
