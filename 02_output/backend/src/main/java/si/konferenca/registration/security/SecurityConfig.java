package si.konferenca.registration.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import si.konferenca.registration.infrastructure.config.AppSettings;

/**
 * Organizer access with HTTP Basic (BR-08, BR-09, SB-02, SB-03); everything else under /api is
 * public. Stateless: no session, no cookies, so CSRF protection is not needed.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  static final String ORGANIZER = "ORGANIZER";

  @Bean
  public SecurityFilterChain apiSecurity(HttpSecurity http, JsonAuthenticationEntryPoint entryPoint)
      throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.GET, "/api/form-config")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/registrations")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/registrations", "/api/export")
                    .hasRole(ORGANIZER)
                    .requestMatchers("/actuator/health", "/actuator/health/**", "/error")
                    .permitAll()
                    .anyRequest()
                    .denyAll())
        .httpBasic(basic -> basic.authenticationEntryPoint(entryPoint))
        .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint))
        .headers(
            headers ->
                headers
                    .frameOptions(frame -> frame.deny())
                    .contentSecurityPolicy(
                        csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                    .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER)));
    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /** The one organizer account; only the BCrypt hash of the password is kept (SB-03). */
  @Bean
  public UserDetailsService organizerAccount(AppSettings settings, PasswordEncoder encoder) {
    AppSettings.Organizer organizer = settings.organizer();
    return new InMemoryUserDetailsManager(
        User.withUsername(organizer.username())
            .password(encoder.encode(organizer.password()))
            .roles(ORGANIZER)
            .build());
  }
}
