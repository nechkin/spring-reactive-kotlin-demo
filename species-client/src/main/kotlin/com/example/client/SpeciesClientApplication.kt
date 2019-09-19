package com.example.client

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.gateway.filter.ratelimit.PrincipalNameKeyResolver
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.reactive.function.client.WebClient


data class Species(val id: Int, val name: String)

@SpringBootApplication
class SpeciesClientApplication {

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
                    .setPath("/species")
                }
                .uri("http://localhost:8080")
            }
            .route { it
                .path("/proxy")
                .filters { filterSpec -> filterSpec
                    .setPath("/species")
                    .requestRateLimiter { config -> config
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(PrincipalNameKeyResolver())
                        // same as below:
//                        .setKeyResolver { exchange -> exchange
//                            .getPrincipal<Principal>()
//                            .map<String>(Principal::getName)
//                            .switchIfEmpty(Mono.empty())
//                        }
                    }
                }
                .uri("http://localhost:8080")
            }
            .build()
    }

    /**
     * Used to limit the rate of incoming requests
     */
    @Bean
    internal fun redisRateLimiter(): RedisRateLimiter {
        // https://en.wikipedia.org/wiki/Leaky_bucket
        // From RedisRateLimiter#isAllowed
        // How many requests per second do you want a user to be allowed to do?
        //int replenishRate = routeConfig.getReplenishRate();

        // How much bursting do you want to allow?
        //int burstCapacity = routeConfig.getBurstCapacity();

        return RedisRateLimiter(5, 7)
    }


    @Bean
    internal fun webClient(): WebClient {
        return WebClient.builder().build()
    }


}

fun main(args: Array<String>) {
    runApplication<SpeciesClientApplication>(*args)
}
