package com.example.reservationserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicates
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Flux

@SpringBootApplication
open class Application {

    @Bean
    open fun routes(): RouterFunction<ServerResponse> {
        return RouterFunctions.route(
            RequestPredicates.GET("/reservations"),
            HandlerFunction { ServerResponse.ok().body(Flux.just("hello"), String::class.java) }
        )
    }
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
