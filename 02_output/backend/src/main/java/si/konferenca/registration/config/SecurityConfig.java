package si.konferenca.registration.config;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Access control (SB-02, BR-08) and security headers (SB-10). Participants are anonymous; the
 * single organizer authenticates with HTTP Basic. Stateless, no cookies, so CSRF is off.
 */
@Configuration
public class SecurityConfig {

  static final String ORGANIZER_ROLE = "ORGANIZER";

  @Bean
  SecurityFilterChain apiSecurity(HttpSecurity http, AppProperties properties) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .headers(
            headers ->
                headers
                    .contentSecurityPolicy(
                        csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                    .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER)))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.GET, "/api/form-config")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/registrations")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/export")
                    .access(new OrganizerAccess(properties.organizer().httpsOnly()))
                    .requestMatchers("/actuator/health", "/actuator/health/**", "/error")
                    .permitAll()
                    .anyRequest()
                    .denyAll())
        .cors(cors -> cors.configurationSource(corsSource(properties.cors().allowedOrigin())))
        .httpBasic(Customizer.withDefaults())
        .exceptionHandling(
            e -> e.accessDeniedHandler((request, response, denied) -> refuse(response)));
    return http.build();
  }

  /** CORS for the one configured frontend origin (local development only); none by default. */
  static CorsConfigurationSource corsSource(String allowedOrigin) {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    if (allowedOrigin != null && !allowedOrigin.isBlank()) {
      CorsConfiguration cors = new CorsConfiguration();
      cors.setAllowedOrigins(List.of(allowedOrigin.strip()));
      cors.setAllowedMethods(List.of("GET", "POST"));
      cors.setAllowedHeaders(List.of("Content-Type", "Authorization"));
      source.registerCorsConfiguration("/api/**", cors);
    }
    return source;
  }

  /** An authenticated organizer on a refused transport (SR-06): 403, no data. */
  static void refuse(HttpServletResponse response) throws IOException {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write("{\"message\":\"Organizer access requires HTTPS.\"}");
  }

  /** The organizer account; only the BCrypt hash of the password is kept (SB-03). */
  @Bean
  UserDetailsService organizerAccount(AppProperties properties, PasswordEncoder encoder) {
    AppProperties.Organizer organizer = properties.organizer();
    if (organizer.username() == null
        || organizer.username().isBlank()
        || organizer.password() == null
        || organizer.password().isBlank()) {
      throw new IllegalStateException("ORGANIZER_USERNAME and ORGANIZER_PASSWORD must be set");
    }
    return new InMemoryUserDetailsManager(
        User.withUsername(organizer.username())
            .password(encoder.encode(organizer.password()))
            .roles(ORGANIZER_ROLE)
            .build());
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
