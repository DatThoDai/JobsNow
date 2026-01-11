package com.JobsNow.backend.config;

import com.JobsNow.backend.filter.AuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationFilter authenticationFilter) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(request -> {
                    // PUBLIC
                    request.requestMatchers("/auth/**").permitAll();
                    request.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll();

                    request.requestMatchers(HttpMethod.GET, "/job", "/job/{jobId}", "/job/searchJobs").permitAll();
                    request.requestMatchers(HttpMethod.GET, "/job/company/{companyId}").permitAll();

                    request.requestMatchers(HttpMethod.GET, "/category/**", "/industry/**", "/skill/**").permitAll();

                    request.requestMatchers(HttpMethod.GET, "/company/**").permitAll();

                    // ROLE-BASED
                    // ADMIN
                    request.requestMatchers("/job/approve/**").hasRole("ADMIN");
                    request.requestMatchers(HttpMethod.PUT, "/job/reject").hasRole("ADMIN");

                    // COMPANY
                    request.requestMatchers("/job/create").hasRole("COMPANY");
                    request.requestMatchers(HttpMethod.PUT, "/job/{jobId}").hasRole("COMPANY");
                    request.requestMatchers(HttpMethod.DELETE, "/job/{jobId}").hasRole("COMPANY");
                    request.requestMatchers("/application/job/**").hasRole("COMPANY");
                    request.requestMatchers(HttpMethod.PUT, "/application/{applicationId}/status").hasRole("COMPANY");

                    // JOBSEEKER
                    request.requestMatchers("/resume/**").hasRole("JOBSEEKER");
                    request.requestMatchers("/application/apply").hasRole("JOBSEEKER");
                    request.requestMatchers("/application/jobseeker/**").hasRole("JOBSEEKER");
                    request.requestMatchers("/profile/**").hasRole("JOBSEEKER");

                    request.requestMatchers(HttpMethod.GET, "/application/{applicationId}").authenticated();

                    request.anyRequest().authenticated();
                })
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
