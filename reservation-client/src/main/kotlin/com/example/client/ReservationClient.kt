package com.example.client

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.web.server.SecurityWebFilterChain


data class Reservation(val id: Int, val name: String)

@SpringBootApplication
class ReservationClient {

    @Bean
    fun authentication() = MapReactiveUserDetailsService(
        User.withDefaultPasswordEncoder()
            .username("user")
            .password("pass")
            .roles("USER")
            .build())

    @Bean
    fun authorization(http: ServerHttpSecurity): SecurityWebFilterChain {
        http.httpBasic()
        http.authorizeExchange()
            .pathMatchers("/proxy").authenticated()
            .anyExchange().permitAll()
        return http.build()
    }

    @Bean
    internal fun routeLocator(routeLocatorBuilder: RouteLocatorBuilder): RouteLocator {
        return routeLocatorBuilder
            .routes()
            .route { routeSpec -> routeSpec
                .header("X-CUSTOM-HEADER", "val.*")
                .and().predicate { Math.random() < .5 }
                .and().path("/unstable-proxy")
                .filters { filterSpec -> filterSpec
                    .setPath("/reservations")
                }
                .uri("http://localhost:8080")
            }
            .build()
    }
}

fun main(args: Array<String>) {
    runApplication<ReservationClient>(*args)
}
