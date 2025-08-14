package org.example;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        // Erlaubt alle Anfragen, um die Endpunkte leicht testen zu können
        httpSecurity.authorizeRequests().anyRequest().permitAll();

        // VULNERABILITY (Row 17: Missing CSRF Protection)
        // Deaktiviert den Schutz gegen Cross-Site Request Forgery.
        httpSecurity.csrf().disable();

        // Bestehende Schwachstelle für die Demo
        // VULNERABILITY (Row 16: Missing Frame Protection / Clickjacking)
        httpSecurity.headers().frameOptions().disable();

        // VULNERABILITY (Row 19: HTML5 XSS Protection Disabled)
        // Deaktiviert den Browser-internen XSS-Filter (obwohl modernere Browser
        // diesen zugunsten von Content-Security-Policy ignorieren, wird es von
        // SAST-Tools als schlechte Praxis erkannt).
        httpSecurity.headers().xssProtection().disable();
    }
}