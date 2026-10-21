package si.konferenca.registration.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import si.konferenca.registration.RegistrationApplication;
import si.konferenca.registration.acceptance.support.TestStack;

/** Security headers, CORS, request errors and limits of the running backend (SB-10, SR-03). */
@SpringBootTest(classes = RegistrationApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles({"test", "local"})
class WebSecurityIntegrationTest {

  private static final String ORIGIN = "http://localhost:5173";

  @Autowired private MockMvc mvc;

  @DynamicPropertySource
  static void stack(DynamicPropertyRegistry registry) {
    TestStack.register(
        registry,
        Map.of(
            "app.cors.allowed-origin", ORIGIN,
            "app.rate-limit.per-minute", "3",
            "app.request.max-bytes", "200"));
  }

  @Test
  void responsesCarryTheSecurityHeaders() throws Exception {
    mvc.perform(get("/api/form-config").with(r -> remote(r, "10.1.0.1")))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"))
        .andExpect(header().string("X-Frame-Options", "DENY"))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().string("Referrer-Policy", "no-referrer"))
        .andExpect(header().string("Content-Type", "application/json;charset=UTF-8"));
  }

  @Test
  void configuredOriginMayCallTheApiAndOthersMayNot() throws Exception {
    mvc.perform(
            options("/api/registrations")
                .with(r -> remote(r, "10.1.0.2"))
                .header("Origin", ORIGIN)
                .header("Access-Control-Request-Method", "POST"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", ORIGIN));
    mvc.perform(
            options("/api/registrations")
                .with(r -> remote(r, "10.1.0.3"))
                .header("Origin", "https://evil.example")
                .header("Access-Control-Request-Method", "POST"))
        .andExpect(status().isForbidden());
  }

  @Test
  void malformedJsonIsAFieldErrorForTheBody() throws Exception {
    mvc.perform(
            post("/api/registrations")
                .with(r -> remote(r, "10.1.0.4"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\": "))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("body"));
    mvc.perform(
            post("/api/registrations")
                .with(r -> remote(r, "10.1.0.4"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"options\": \"ws-ai\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("body"));
  }

  @Test
  void otherContentTypesAreA415() throws Exception {
    mvc.perform(
            post("/api/registrations")
                .with(r -> remote(r, "10.1.0.5"))
                .contentType(MediaType.TEXT_PLAIN)
                .content("hello"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.message").value("Send the request as application/json."));
  }

  @Test
  void bodiesOverTheLimitAreA413() throws Exception {
    mvc.perform(
            post("/api/registrations")
                .with(r -> remote(r, "10.1.0.6"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\": \"" + "x".repeat(300) + "\"}"))
        .andExpect(status().isContentTooLarge());
  }

  @Test
  void theFourthRequestInAMinuteIsA429() throws Exception {
    for (int i = 0; i < 3; i++) {
      mvc.perform(get("/api/form-config").with(r -> remote(r, "10.1.0.7")))
          .andExpect(status().isOk());
    }
    mvc.perform(get("/api/form-config").with(r -> remote(r, "10.1.0.7")))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"));
  }

  @Test
  void unknownEndpointsAreDenied() throws Exception {
    mvc.perform(get("/api/registrations").with(r -> remote(r, "10.1.0.8")))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/actuator/health").with(r -> remote(r, "10.1.0.8")))
        .andExpect(status().isOk());
  }

  private static org.springframework.mock.web.MockHttpServletRequest remote(
      org.springframework.mock.web.MockHttpServletRequest request, String address) {
    request.setRemoteAddr(address);
    return request;
  }
}
