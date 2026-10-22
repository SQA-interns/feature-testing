package si.konferenca.registration.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import si.konferenca.registration.adapter.web.OrganizerAuthenticationEntryPoint;

/**
 * HTTP Basic for the organizer endpoints with a BCrypt-hashed password (SB-02, SB-03, PA-04);
 * everything else public or denied; security headers (SB-10).
 */
@Configuration
public class SecurityConfiguration {

  static final String ORGANIZER = "ORGANIZER";

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService organizers(AppSettings settings, PasswordEncoder encoder) {
    return new InMemoryUserDetailsManager(
        User.withUsername(settings.organizer().username())
            .password(encoder.encode(settings.organizer().password()))
            .roles(ORGANIZER)
            .build());
  }

  @Bean
  SecurityFilterChain apiSecurity(HttpSecurity http, AppSettings settings) throws Exception {
    OrganizerAuthenticationEntryPoint entryPoint = new OrganizerAuthenticationEntryPoint();
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .httpBasic(basic -> basic.authenticationEntryPoint(entryPoint))
        .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint))
        .authorizeHttpRequests(
            requests ->
                requests
                    .requestMatchers(HttpMethod.GET, "/api/export", "/api/registrations")
                    .hasRole(ORGANIZER)
                    .requestMatchers(HttpMethod.GET, "/api/form-config")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/registrations")
                    .permitAll()
                    .requestMatchers("/actuator/health", "/actuator/health/**", "/error")
                    .permitAll()
                    .anyRequest()
                    .denyAll())
        .headers(
            headers ->
                headers
                    .contentSecurityPolicy(
                        csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                    .frameOptions(frame -> frame.deny())
                    .referrerPolicy(
                        referrer ->
                            referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)));
    String origin = settings.corsAllowedOrigin();
    if (origin != null && !origin.isBlank()) {
      CorsConfiguration cors = new CorsConfiguration();
      cors.setAllowedOrigins(List.of(origin.trim()));
      cors.setAllowedMethods(List.of("GET", "POST"));
      cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
      UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
      source.registerCorsConfiguration("/api/**", cors);
      http.cors(c -> c.configurationSource(source));
    } else {
      http.cors(c -> c.disable());
    }
    return http.build();
  }
}
