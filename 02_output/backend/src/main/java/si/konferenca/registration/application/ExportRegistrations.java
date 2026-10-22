package si.konferenca.registration.application;

/** The organizer export of every registration (US-008, BR-08). */
public class ExportRegistrations {

  private final RegistrationQueries store;
  private final WorkbookWriter writer;

  public ExportRegistrations(RegistrationQueries store, WorkbookWriter writer) {
    this.store = store;
    this.writer = writer;
  }

  public byte[] export() {
    return writer.write(store.all());
  }
}
