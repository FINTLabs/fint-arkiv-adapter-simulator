package no.novari.flyt.archive.simulator.security

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import org.springframework.validation.annotation.Validated

@ConfigurationProperties(prefix = "simulator.admin")
@Validated
data class AdminAuthProperties(
    @field:NotBlank var username: String = "",
    @field:NotBlank var password: String = "",
)

@Configuration
@EnableConfigurationProperties(AdminAuthProperties::class)
class AdminSecurityConfig(
    private val properties: AdminAuthProperties,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/internal/**")
                    .authenticated()
                    .anyRequest()
                    .permitAll()
            }.httpBasic(Customizer.withDefaults())
        return http.build()
    }

    @Bean
    fun userDetailsService(passwordEncoder: PasswordEncoder): UserDetailsService {
        val user =
            User
                .builder()
                .username(properties.username)
                .password(passwordEncoder.encode(properties.password))
                .roles("ADMIN")
                .build()
        return InMemoryUserDetailsManager(user)
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder()
    }
}
