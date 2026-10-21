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

/** Reads the storage interface of {@code docs/02_contracts/database.json} with plain JDBC. */
public final class Database {

  private static final List<String> TEXT_COLUMNS =
      List.of(
          "type",
          "first_name",
          "last_name",
          "email",
          "organization",
          "study_institution",
          "study_programme",
          "student_id");

  private final String url;
  private final String user;
  private final String password;

  Database(String url, String user, String password) {
    this.url = url;
    this.user = user;
    this.password = password;
  }

  /** Rows of {@code registration} whose email equals the given value. */
  public long countByEmail(String email) {
    return count("select count(*) from registration where email = ?", email);
  }

  public long countAll() {
    return count("select count(*) from registration");
  }

  /** The text columns of one registration row, or an empty map when the id is unknown. */
  public Map<String, String> registration(String id) {
    Map<String, String> row = new LinkedHashMap<>();
    query(
        "select * from registration where id = ?",
        rs -> {
          if (rs.next()) {
            for (String column : TEXT_COLUMNS) {
              row.put(column, rs.getString(column));
            }
          }
        },
        UUID.fromString(id));
    return row;
  }

  public OffsetDateTime registeredAt(String id) {
    List<OffsetDateTime> values = new ArrayList<>();
    query(
        "select registered_at from registration where id = ?",
        rs -> {
          while (rs.next()) {
            values.add(rs.getObject(1, OffsetDateTime.class));
          }
        },
        UUID.fromString(id));
    return values.isEmpty() ? null : values.get(0);
  }

  /** Selected option ids in stored order. */
  public List<String> options(String id) {
    List<String> values = new ArrayList<>();
    query(
        "select option_id from registration_option where registration_id = ? order by position",
        rs -> {
          while (rs.next()) {
            values.add(rs.getString(1));
          }
        },
        UUID.fromString(id));
    return values;
  }

  /** Consent id to the time it was given. */
  public Map<String, OffsetDateTime> consents(String id) {
    Map<String, OffsetDateTime> values = new LinkedHashMap<>();
    query(
        "select consent_id, given_at from registration_consent where registration_id = ?",
        rs -> {
          while (rs.next()) {
            values.put(rs.getString(1), rs.getObject(2, OffsetDateTime.class));
          }
        },
        UUID.fromString(id));
    return values;
  }

  private long count(String sql, Object... args) {
    long[] result = new long[1];
    query(
        sql,
        rs -> {
          rs.next();
          result[0] = rs.getLong(1);
        },
        args);
    return result[0];
  }

  private void query(String sql, ResultHandler handler, Object... args) {
    try (Connection c = DriverManager.getConnection(url, user, password);
        PreparedStatement s = c.prepareStatement(sql)) {
      for (int i = 0; i < args.length; i++) {
        s.setObject(i + 1, args[i]);
      }
      try (ResultSet rs = s.executeQuery()) {
        handler.handle(rs);
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Query failed: " + sql, e);
    }
  }

  @FunctionalInterface
  private interface ResultHandler {
    void handle(ResultSet rs) throws SQLException;
  }
}
