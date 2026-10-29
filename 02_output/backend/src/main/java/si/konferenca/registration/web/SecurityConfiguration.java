package si.konferenca.registration.web;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import si.konferenca.registration.config.AppProperties;

/**
 * Access rules of specification S7.2: public form configuration and registration, the export and
 * the organizer search only for the organizer over HTTP Basic, stateless, no cookies; security
 * headers (SB-10).
 */
@Configuration
public class SecurityConfiguration {

  private static final int BCRYPT_STRENGTH = 12;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, AppProperties properties)
      throws Exception {
    JsonAuthenticationEntryPoint entryPoint = new JsonAuthenticationEntryPoint();
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .httpBasic(b -> b.realmName("registration-export").authenticationEntryPoint(entryPoint))
        .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(HttpMethod.GET, "/api/form-config")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/registrations")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET, HttpsOnlyFilter.EXPORT_PATH, HttpsOnlyFilter.SEARCH_PATH)
                    .hasRole("ORGANIZER")
                    .requestMatchers("/actuator/health", "/actuator/health/**", "/error")
                    .permitAll()
                    .anyRequest()
                    .denyAll())
        .headers(
            h ->
                h.contentSecurityPolicy(
                        c -> c.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                    .frameOptions(f -> f.deny())
                    .referrerPolicy(
                        r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
        .addFilterBefore(
            new HttpsOnlyFilter(properties.organizer().httpsOnly()),
            BasicAuthenticationFilter.class);
    String origin = properties.corsAllowedOrigin();
    if (origin != null && !origin.isBlank()) {
      CorsConfiguration cors = new CorsConfiguration();
      cors.setAllowedOrigins(List.of(origin));
      cors.setAllowedMethods(List.of("GET", "POST"));
      cors.setAllowedHeaders(List.of("Content-Type", "Authorization"));
      UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
      source.registerCorsConfiguration("/api/**", cors);
      http.cors(c -> c.configurationSource(source));
    }
    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
  }

  /** The organizer account; only the BCrypt hash of the password is kept (SB-03). */
  @Bean
  public UserDetailsService organizerAccount(AppProperties properties, PasswordEncoder encoder) {
    AppProperties.Organizer organizer = properties.organizer();
    return new InMemoryUserDetailsManager(
        User.withUsername(organizer.username())
            .password(encoder.encode(organizer.password()))
            .roles("ORGANIZER")
            .build());
  }
}
