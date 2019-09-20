package com.example.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.rsocket.RSocket
import io.rsocket.RSocketFactory
import io.rsocket.frame.decoder.PayloadDecoder
import io.rsocket.transport.netty.client.TcpClientTransport
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.gateway.filter.ratelimit.PrincipalNameKeyResolver
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.util.MimeTypeUtils
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration


//data class Species @JsonCreator constructor(@JsonProperty("id") val id: Int,
//                                            @JsonProperty("name") val name: String)

// Don't nee @Json annotation with jackson kotlin module
data class Species constructor(val id: Int, val name: String)

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

    @Bean
    internal fun tcpClientTransport() =
            TcpClientTransport.create(7070)

    @Bean
    internal fun objectMapper() = ObjectMapper().registerKotlinModule()

    @Bean
    fun rSocket(): RSocket? {
        return RSocketFactory
            .connect()
            .mimeType(MimeTypeUtils.APPLICATION_JSON_VALUE, MimeTypeUtils.APPLICATION_JSON_VALUE)
            .frameDecoder(PayloadDecoder.ZERO_COPY)
            .transport(TcpClientTransport.create(7000))
            .start()
            .retryBackoff(100, Duration.ofSeconds(1), Duration.ofSeconds(30))
            .block()
    }

    @Bean
    fun rSocketRequester(rSocketStrategies: RSocketStrategies): RSocketRequester {
        return RSocketRequester.wrap(rSocket()!!, MimeTypeUtils.APPLICATION_JSON, rSocketStrategies)
    }
}

fun main(args: Array<String>) {
    runApplication<SpeciesClientApplication>(*args)
}
