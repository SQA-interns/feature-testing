package si.konferenca.registration.acceptance.support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Reads the registration tables of {@code docs/02_contracts/database.md} with plain JDBC. */
public final class Database {

  /** One row of {@code registration}. */
  public record Row(
      UUID id,
      String type,
      String firstName,
      String lastName,
      String email,
      String organization,
      String studyInstitution,
      String studyProgramme,
      String studentId,
      OffsetDateTime registeredAt) {}

  private Connection connect() throws SQLException {
    return DriverManager.getConnection(
        Containers.POSTGRES.getJdbcUrl(),
        Containers.POSTGRES.getUsername(),
        Containers.POSTGRES.getPassword());
  }

  /** Empties the registration tables, if the schema exists yet. */
  public void clear() {
    if (!schemaExists()) {
      return;
    }
    update("TRUNCATE registration_option, registration_consent, registration");
  }

  public boolean schemaExists() {
    return queryOne("SELECT to_regclass('public.registration') IS NOT NULL", Boolean.class);
  }

  public long countRegistrations() {
    if (!schemaExists()) {
      return 0;
    }
    return queryOne("SELECT count(*) FROM registration", Long.class);
  }

  public List<Row> rows() {
    List<Row> rows = new ArrayList<>();
    query(
        "SELECT id, type, first_name, last_name, email, organization, study_institution,"
            + " study_programme, student_id, registered_at FROM registration ORDER BY registered_at",
        rs ->
            rows.add(
                new Row(
                    rs.getObject("id", UUID.class),
                    rs.getString("type"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("organization"),
                    rs.getString("study_institution"),
                    rs.getString("study_programme"),
                    rs.getString("student_id"),
                    rs.getObject("registered_at", OffsetDateTime.class))));
    return rows;
  }

  public Row row(UUID id) {
    return rows().stream()
        .filter(r -> r.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new AssertionError("no registration row " + id));
  }

  public List<String> optionIds(UUID id) {
    List<String> ids = new ArrayList<>();
    query(
        "SELECT option_id FROM registration_option WHERE registration_id = ? ORDER BY option_id",
        rs -> ids.add(rs.getString(1)),
        id);
    return ids;
  }

  /** Consent id to the time it was given. */
  public Map<String, OffsetDateTime> consents(UUID id) {
    Map<String, OffsetDateTime> consents = new LinkedHashMap<>();
    query(
        "SELECT consent_id, given_at FROM registration_consent WHERE registration_id = ?"
            + " ORDER BY consent_id",
        rs -> consents.put(rs.getString(1), rs.getObject(2, OffsetDateTime.class)),
        id);
    return consents;
  }

  private interface RowHandler {
    void handle(ResultSet rs) throws SQLException;
  }

  private void query(String sql, RowHandler handler, Object... parameters) {
    try (Connection connection = connect();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int i = 0; i < parameters.length; i++) {
        statement.setObject(i + 1, parameters[i]);
      }
      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          handler.handle(rs);
        }
      }
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  private <T> T queryOne(String sql, Class<T> type) {
    List<T> values = new ArrayList<>();
    query(sql, rs -> values.add(rs.getObject(1, type)));
    return values.getFirst();
  }

  private void update(String sql) {
    try (Connection connection = connect();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.executeUpdate();
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }
}
