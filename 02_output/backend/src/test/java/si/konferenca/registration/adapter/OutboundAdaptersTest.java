package si.konferenca.registration.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import si.konferenca.registration.adapter.export.ExcelRegistrationExporter;
import si.konferenca.registration.adapter.jsoncopy.FileJsonCopyStore;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** JSON copy files and the Excel workbook. */
class OutboundAdaptersTest {

  static final Instant AT = Instant.parse("2026-10-20T10:00:00.123456Z");

  static Registration student() {
    return new Registration(
        UUID.fromString("11111111-2222-4333-8444-555555555555"),
        RegistrationType.STUDENT,
        "Špela",
        "=HYPERLINK(\"x\")",
        "s@x.org",
        null,
        "Uni",
        "CS",
        "123",
        List.of("b", "a"),
        List.of(new GivenConsent("privacy", AT), new GivenConsent("photos", AT)),
        AT);
  }

  @Test
  void jsonCopyIsWrittenAtomicallyWithTheSchemaFields(@TempDir Path dir) throws Exception {
    Path target = dir.resolve("copies");
    FileJsonCopyStore store = new FileJsonCopyStore(target);

    byte[] written = store.write(student());

    Path file = target.resolve("11111111-2222-4333-8444-555555555555.json");
    assertThat(Files.readAllBytes(file)).isEqualTo(written);
    try (var files = Files.list(target)) {
      assertThat(files).containsExactly(file);
    }
    JsonNode copy = JsonMapper.builder().build().readTree(written);
    assertThat(copy.propertyNames())
        .containsExactly(
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
    assertThat(copy.path("firstName").asString()).isEqualTo("Špela");
    assertThat(copy.path("organization").isNull()).isTrue();
    assertThat(copy.path("options").toString()).isEqualTo("[\"b\",\"a\"]");
    assertThat(copy.path("consents").toString())
        .isEqualTo(
            "[{\"id\":\"privacy\",\"givenAt\":\"2026-10-20T10:00:00.123456Z\"},"
                + "{\"id\":\"photos\",\"givenAt\":\"2026-10-20T10:00:00.123456Z\"}]");
    assertThat(copy.path("registeredAt").asString()).isEqualTo("2026-10-20T10:00:00.123456Z");
    assertThat(new String(written, StandardCharsets.UTF_8)).contains("Špela");
  }

  @Test
  void deleteRemovesTheCopyAndIgnoresMissingOnes(@TempDir Path dir) {
    FileJsonCopyStore store = new FileJsonCopyStore(dir);
    store.write(student());

    store.delete(student().id());
    store.delete(UUID.randomUUID());

    assertThat(dir.resolve(student().id() + ".json")).doesNotExist();
  }

  @Test
  void unwritableDirectoryFailsWithoutLeavingFiles(@TempDir Path dir) throws Exception {
    Path notADirectory = Files.writeString(dir.resolve("file"), "x");
    FileJsonCopyStore store = new FileJsonCopyStore(notADirectory);

    assertThatThrownBy(() -> store.write(student())).isInstanceOf(UncheckedIOException.class);
    assertThat(Files.readString(notADirectory)).isEqualTo("x");
  }

  @Test
  void failedMoveLeavesNoTemporaryFile(@TempDir Path dir) throws Exception {
    Path occupied = dir.resolve(student().id() + ".json");
    Files.createDirectories(occupied.resolve("inside"));
    FileJsonCopyStore store = new FileJsonCopyStore(dir);

    assertThatThrownBy(() -> store.write(student())).isInstanceOf(UncheckedIOException.class);
    try (var files = Files.list(dir)) {
      assertThat(files).containsExactly(occupied);
    }
  }

  @Test
  void deleteOfAnUndeletablePathOnlyLogs(@TempDir Path dir) throws Exception {
    Path directoryWithContent = dir.resolve(student().id() + ".json");
    Files.createDirectories(directoryWithContent.resolve("inside"));

    new FileJsonCopyStore(dir).delete(student().id());

    assertThat(directoryWithContent).isDirectory();
  }

  @Test
  void workbookHasTheHeaderAndStringCells() throws Exception {
    byte[] bytes = new ExcelRegistrationExporter().export(List.of(student()));

    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
      Sheet sheet = workbook.getSheetAt(0);
      assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
      assertThat(sheet.getSheetName()).isEqualTo("registrations");
      assertThat(sheet.getLastRowNum()).isEqualTo(1);
      Row header = sheet.getRow(0);
      assertThat(header.getCell(0).getStringCellValue()).isEqualTo("id");
      assertThat(header.getCell(11).getStringCellValue()).isEqualTo("registeredAt");
      Row row = sheet.getRow(1);
      assertThat(row.getCell(3).getCellType()).isEqualTo(CellType.STRING);
      assertThat(row.getCell(3).getStringCellValue()).isEqualTo("=HYPERLINK(\"x\")");
    }
  }

  @Test
  void rowValuesFollowTheContract() {
    assertThat(exporterValues(student()))
        .containsExactly(
            "11111111-2222-4333-8444-555555555555",
            "STUDENT",
            "Špela",
            "=HYPERLINK(\"x\")",
            "s@x.org",
            "",
            "Uni",
            "CS",
            "123",
            "b, a",
            "privacy, photos",
            "2026-10-20T10:00:00.123456Z");
  }

  @Test
  void externalRowLeavesStudentCellsEmpty() {
    Registration external =
        new Registration(
            UUID.randomUUID(),
            RegistrationType.EXTERNAL,
            "A",
            "B",
            "a@x.org",
            "Org",
            null,
            null,
            null,
            List.of(),
            List.of(),
            AT);

    List<String> values = exporterValues(external);
    assertThat(values.subList(5, 11)).containsExactly("Org", "", "", "", "", "");
  }

  static List<String> exporterValues(Registration registration) {
    try (XSSFWorkbook workbook =
        new XSSFWorkbook(
            new ByteArrayInputStream(
                new ExcelRegistrationExporter().export(List.of(registration))))) {
      Row row = workbook.getSheetAt(0).getRow(1);
      return java.util.stream.IntStream.range(0, 12)
          .mapToObj(c -> row.getCell(c).getStringCellValue())
          .toList();
    } catch (java.io.IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
