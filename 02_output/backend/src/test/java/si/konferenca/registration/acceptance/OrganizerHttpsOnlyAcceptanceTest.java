package si.konferenca.registration.acceptance;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import si.konferenca.registration.acceptance.support.AcceptanceTest;
import si.konferenca.registration.acceptance.support.Submissions;
import si.konferenca.registration.acceptance.support.TestStack;

/** US-008, SR-06: with HTTPS-only organizer access on, plain HTTP from a remote client fails. */
@AcceptanceTest
class OrganizerHttpsOnlyAcceptanceTest {

  private static final String REMOTE_CLIENT = "203.0.113.10";

  @Autowired private MockMvc mvc;

  @DynamicPropertySource
  static void stack(DynamicPropertyRegistry registry) {
    TestStack.register(registry, Map.of("app.organizer.https-only", "true"));
  }

  @Test
  void ac_008_04_validCredentialsOverPlainHttpFromRemoteClientAreRefusedWithoutData()
      throws Exception {
    Map<String, Object> body = Submissions.validExternal();
    mvc.perform(Submissions.post(body)).andExpect(status().isCreated());

    MockHttpServletResponse response =
        mvc.perform(Submissions.export().with(client(REMOTE_CLIENT, false)))
            .andExpect(status().isForbidden())
            .andReturn()
            .getResponse();

    ExportAcceptanceTest.assertNoRegistrationData(response, (String) body.get("email"));
  }

  @Test
  void ac_008_04_validCredentialsOverHttpsFromRemoteClientAreAccepted() throws Exception {
    mvc.perform(Submissions.export().with(client(REMOTE_CLIENT, true))).andExpect(status().isOk());
  }

  @Test
  void ac_008_04_validCredentialsOverPlainHttpFromLocalhostAreAccepted() throws Exception {
    mvc.perform(Submissions.export().with(client("127.0.0.1", false))).andExpect(status().isOk());
  }

  @Test
  void ac_008_04_missingCredentialsOverPlainHttpFromRemoteClientAreStillUnauthorized()
      throws Exception {
    mvc.perform(Submissions.export(null, null).with(client(REMOTE_CLIENT, false)))
        .andExpect(status().isUnauthorized());
  }

  private static RequestPostProcessor client(String address, boolean secure) {
    return request -> {
      request.setRemoteAddr(address);
      request.setSecure(secure);
      request.setScheme(secure ? "https" : "http");
      request.setServerPort(secure ? 443 : 80);
      return request;
    };
  }
}
