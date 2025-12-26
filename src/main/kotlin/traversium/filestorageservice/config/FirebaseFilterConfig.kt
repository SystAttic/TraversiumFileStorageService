package traversium.filestorageservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import traversium.filestorageservice.security.FirebaseAuthenticationFilter

@Configuration
@EnableWebSecurity
class FirebaseFilterConfig {

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        firebaseAuthenticationFilter: FirebaseAuthenticationFilter
    ): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/swagger-ui.html"
                    ).permitAll()
                    .requestMatchers("/actuator/health/**").permitAll()
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/actuator/prometheus/**").permitAll()
                    .requestMatchers("/actuator/prometheus").permitAll()
                    .requestMatchers("/actuator/**").permitAll()
                    .requestMatchers("/graphql").authenticated()
                    .requestMatchers("/rest/**").authenticated()

                    .anyRequest().permitAll()
            }
            .addFilterBefore(firebaseAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}