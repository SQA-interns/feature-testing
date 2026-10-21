package si.konferenca.registration.acceptance.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import si.konferenca.registration.RegistrationApplication;

/**
 * The whole backend started on its public HTTP interface (MockMvc) in the profile {@code test}.
 * Each test class adds the stack settings with {@code TestStack.register} in its own
 * {@code @DynamicPropertySource}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = RegistrationApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public @interface AcceptanceTest {}
