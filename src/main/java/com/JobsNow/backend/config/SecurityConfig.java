package com.JobsNow.backend.config;

import com.JobsNow.backend.filter.AuthenticationFilter;
import com.JobsNow.backend.response.ResponseFactory;
import com.JobsNow.backend.service.SavedJobService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
                    request.requestMatchers(HttpMethod.GET, "/skill/all").permitAll();
                    request.requestMatchers(HttpMethod.GET, "/category/**", "/industry/**", "/skill/**").permitAll();
                    request.requestMatchers(HttpMethod.GET, "/company/me").hasRole("COMPANY");
                    request.requestMatchers(HttpMethod.POST, "/company/me").hasRole("COMPANY");
                    request.requestMatchers(HttpMethod.PUT, "/company/update/**").hasRole("COMPANY");
                    request.requestMatchers(HttpMethod.GET, "/company/all", "/company/search").permitAll();
                    request.requestMatchers(HttpMethod.GET, "/company/{companyId}").permitAll();
                    request.requestMatchers(HttpMethod.GET, "/company/**").permitAll();
                    request.requestMatchers(HttpMethod.GET, "/category/**").permitAll();
                    request.requestMatchers(HttpMethod.GET, "/profile/{profileId}").permitAll();
                    request.requestMatchers(HttpMethod.GET,"/company/**").permitAll();
                    request.requestMatchers(HttpMethod.GET, "/major/**").permitAll();
                    // ROLE-BASED
                    // ADMIN
                    request.requestMatchers("/job/approve/**").hasRole("ADMIN");
                    request.requestMatchers(HttpMethod.PUT, "/job/reject").hasRole("ADMIN");
                    request.requestMatchers("/skill/add", "/skill/update", "/skill/delete/**").hasRole("ADMIN");
                    request.requestMatchers("/category/add", "/category/update", "/category/delete/**").hasRole("ADMIN");
                    request.requestMatchers(HttpMethod.GET, "/profile/all").hasAnyRole("ADMIN", "COMPANY");
                    request.requestMatchers("/major/**").hasRole("ADMIN");
                    // COMPANY
                    request.requestMatchers("/job/create").hasRole("COMPANY");
                    request.requestMatchers(HttpMethod.PUT, "/job/{jobId}").hasRole("COMPANY");
                    request.requestMatchers(HttpMethod.DELETE, "/job/{jobId}").hasRole("COMPANY");
                    request.requestMatchers("/application/job/**", "/application/company/**").hasRole("COMPANY");
                    request.requestMatchers(HttpMethod.PUT, "/application/{applicationId}/status").hasRole("COMPANY");
                    request.requestMatchers(HttpMethod.POST, "/company/{companyId}/logo").hasRole("COMPANY");
                    request.requestMatchers(HttpMethod.DELETE, "/company/{companyId}/logo").hasRole("COMPANY");
                    request.requestMatchers(HttpMethod.PUT, "/company/{companyId}").hasRole("COMPANY");
                    request.requestMatchers(HttpMethod.POST, "/company/{companyId}/banner").hasRole("COMPANY");
                    request.requestMatchers(HttpMethod.DELETE, "/company/{companyId}/banner").hasRole("COMPANY");
                    request.requestMatchers(HttpMethod.GET, "/company/{companyId}/images").hasRole("COMPANY");
                    request.requestMatchers(HttpMethod.POST, "/company/{companyId}/images").hasRole("COMPANY");
                    request.requestMatchers(HttpMethod.DELETE, "/company/images/**").hasRole("COMPANY");
                    // JOBSEEKER
                    request.requestMatchers("/resume/**").hasRole("JOBSEEKER");
                    request.requestMatchers("/application/apply").hasRole("JOBSEEKER");
                    request.requestMatchers("/application/jobseeker/**").hasRole("JOBSEEKER");
                    request.requestMatchers("/profile/**").hasRole("JOBSEEKER");
                    request.requestMatchers("/profile/user/{userId}").hasRole("JOBSEEKER");
                    request.requestMatchers(HttpMethod.PUT, "/profile/{profileId}").hasRole("JOBSEEKER");
                    request.requestMatchers(HttpMethod.GET, "/application/{applicationId}").authenticated();
                    request.requestMatchers("/profile/{profileId}/avatar").hasRole("JOBSEEKER");
                    request.requestMatchers("/profile/skills").hasRole("JOBSEEKER");
                    request.requestMatchers("/savedJob/**").hasRole("JOBSEEKER");
                    request.anyRequest().authenticated();

                })
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
