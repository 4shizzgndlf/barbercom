
package dk.ek.barbercom.config;

import dk.ek.barbercom.repository.UserRepository;
import dk.ek.barbercom.service.BusinessSubscriptionService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(
            UserRepository userRepository,
            BusinessSubscriptionService businessSubscriptions) {

        return email -> {
            businessSubscriptions.reconcilePendingSubscription(email);
            return userRepository
                .findByEmail(email.trim().toLowerCase())
                .map(user -> User.withUsername(user.getEmail())
                        .password(user.getPassword())
                        .roles(user.getRole().name())
                        .disabled(user.getRole() == dk.ek.barbercom.model.Role.BUSINESS && !user.hasActiveBusinessSubscription())
                        .build())
                .orElseThrow(() ->
                        new UsernameNotFoundException("Bruger blev ikke fundet: " + email));
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                // Sends a CSRF token to the browser as the XSRF-TOKEN cookie.
                // JavaScript sends that token back on POST requests.
                .csrf(csrf -> csrf.csrfTokenRepository(
                        CookieCsrfTokenRepository.withHttpOnlyFalse())
                        // The admin dashboard sends multipart/form-data when a
                        // product image is selected. Keep CSRF for the customer
                        // flows, but let the already role-protected admin API
                        // handle its own multipart posts without a token mismatch.
                        .ignoringRequestMatchers("/api/admin/**")
                        // Stripe signs its webhook requests instead of using our
                        // browser CSRF cookie, so signature verification happens
                        // in StripeWebhookController.
                        .ignoringRequestMatchers("/api/stripe/webhook")
                        // Accept the raw token that JavaScript reads from the cookie.
                        .csrfTokenRequestHandler(
                                new CsrfTokenRequestAttributeHandler()))
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/login",
                                "/login.html",
                                "/register.html",
                                "/shop.html",
                                "/product.html",
                                "/article.html",
                                "/api/auth/register",
                                "/api/auth/status",
                                "/api/stripe/webhook",
                                "/products/**",
                                "/services/**",
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()

                        // Visitors may browse the catalogue without an account.
                        .requestMatchers(HttpMethod.GET,
                                "/api/products/**",
                                "/api/catalog/products",
                                "/api/services/**",
                                "/api/barbers").permitAll()

                        // Buying and booking always require an authenticated user.
                        .requestMatchers(
                                "/checkout/**",
                                "/orders/**",
                                "/bookings/**",
                                "/appointments/**",
                                "/api/orders/**",
                                "/api/checkout/**",
                                "/api/bookings/**",
                                "/api/appointments/**"
                        ).authenticated()

                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/admin.html").hasRole("ADMIN")
                        .requestMatchers("/business/**").hasRole("BUSINESS")

                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login.html")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login.html?error")
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutSuccessUrl("/login.html?logout")
                        .permitAll()
                );

        return http.build();
    }
}
