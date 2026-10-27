package si.konferenca.registration.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import si.konferenca.registration.application.ExportRegistrations;
import si.konferenca.registration.application.GetFormConfiguration;
import si.konferenca.registration.application.RegisterParticipant;
import si.konferenca.registration.config.AppProperties;

/** The web slice with the real security chain and filters; the use cases are mocked. */
@WebMvcTest(controllers = {RegistrationController.class, ExportController.class})
@Import({SecurityConfiguration.class, SecurityWebTest.Settings.class})
class SecurityWebTest {

  static final String ORIGIN = "https://form.konferenca.test";

  /** Boot 4 leaves security out of the MVC slice; this enables the chain under test. */
  @TestConfiguration
  @EnableWebSecurity
  static class Settings {
    @Bean
    AppProperties appProperties() {
      return new AppProperties(
          "test",
          new AppProperties.Conference("C", Path.of("c.json")),
          Path.of("copies"),
          new AppProperties.Recaptcha(true, "", "", ""),
          new AppProperties.Organizer("organizer", "secret-password", "a@x.si", false),
          new AppProperties.Mail("h", 25, false, null, null, "f@x.si"),
          ORIGIN,
          new AppProperties.RateLimit(100, 100, 100),
          10_000);
    }

    @Bean
    Clock clock() {
      return Clock.systemUTC();
    }
  }

  @Autowired MockMvc mvc;
  @MockitoBean GetFormConfiguration formConfiguration;
  @MockitoBean RegisterParticipant registerParticipant;
  @MockitoBean ExportRegistrations exportRegistrations;

  @Test
  void corsPreflightFromTheFormOriginIsAllowed() throws Exception {
    mvc.perform(
            options("/api/registrations")
                .header("Origin", ORIGIN)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", ORIGIN))
        .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST"));
  }

  @Test
  void corsFromAnotherOriginIsRefused() throws Exception {
    mvc.perform(
            options("/api/registrations")
                .header("Origin", "https://evil.example")
                .header("Access-Control-Request-Method", "POST"))
        .andExpect(status().isForbidden());
  }

  @Test
  void exportNeedsTheOrganizerAndSendsAnXlsxAttachment() throws Exception {
    when(exportRegistrations.export()).thenReturn(new byte[] {1, 2});

    mvc.perform(get(HttpsOnlyFilter.EXPORT_PATH))
        .andExpect(status().isUnauthorized())
        .andExpect(content().json("{\"message\":\"Authentication required.\"}"));
    mvc.perform(get(HttpsOnlyFilter.EXPORT_PATH).with(basic("organizer", "wrong")))
        .andExpect(status().isUnauthorized());
    mvc.perform(get(HttpsOnlyFilter.EXPORT_PATH).with(basic("organizer", "secret-password")))
        .andExpect(status().isOk())
        .andExpect(content().contentType(ExportController.XLSX))
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(
            header()
                .string(
                    "Content-Disposition",
                    org.hamcrest.Matchers.matchesPattern(
                        "attachment; filename=\"registrations-\\d{4}-\\d{2}-\\d{2}\\.xlsx\"")))
        .andExpect(content().bytes(new byte[] {1, 2}));
  }

  @Test
  void otherPathsAndMethodsAreDenied() throws Exception {
    mvc.perform(post("/api/form-config")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/other").with(basic("organizer", "secret-password")))
        .andExpect(status().isForbidden());
  }

  @Test
  void responsesCarryTheSecurityHeaders() throws Exception {
    mvc.perform(post("/api/registrations").contentType(MediaType.TEXT_PLAIN).content("x"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(header().string("X-Frame-Options", "DENY"))
        .andExpect(header().string("Referrer-Policy", "no-referrer"))
        .andExpect(
            header()
                .string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"));
  }

  private static org.springframework.test.web.servlet.request.RequestPostProcessor basic(
      String user, String password) {
    return request -> {
      request.addHeader(
          "Authorization",
          "Basic "
              + java.util.Base64.getEncoder()
                  .encodeToString(
                      (user + ":" + password).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
      return request;
    };
  }
}
