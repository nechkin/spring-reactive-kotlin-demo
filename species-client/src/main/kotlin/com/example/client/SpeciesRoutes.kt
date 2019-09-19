package com.example.client

import org.springframework.cloud.netflix.hystrix.HystrixCommands
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicates
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.retry.Retry
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.BiFunction


@Configuration
class SpeciesRoutesConfig(val webClient: WebClient) {

    fun getSpecies(): Flux<Species> {
        return this.webClient
            .get()
            .uri("http://localhost:8080/species")
            .retrieve()
            .bodyToFlux(Species::class.java)
    }

    fun getSpeciesStream(): Flux<Species> {
        return this.webClient
            .get()
            .uri("http://localhost:8080/species-stream")
            .accept(MediaType.APPLICATION_STREAM_JSON)
            .exchange()
            .flatMapMany { clientResponse -> clientResponse.bodyToFlux(Species::class.java) }
             .log()
    }

    @Bean
    fun speciesRoutes(): RouterFunction<ServerResponse> {
        return RouterFunctions.route<ServerResponse>(
            RequestPredicates.GET("/species/names"),
            HandlerFunction { request ->
                val reservationsNames = getSpecies()
                    .map(Species::name)
                    .map { "$it\n" }

                val namesWithBreaker = HystrixCommands
                    .from(reservationsNames)
                    .commandName("speciesNames")
                    .fallback(Flux.just("Problem!"))
                    .eager()
                    .build()

                ServerResponse.ok().body(namesWithBreaker, String::class.java)
            })
            .andRoute(
                RequestPredicates.GET("/species-stream"),
                HandlerFunction { request ->

                    // Trying out retrying and maybe I'll try to retry it on another try

                    // Nice retry builders from project Reactor
                    val retryWhen1 = getSpeciesStream().retryWhen(Retry
                        .any<Throwable>()
                        .fixedBackoff(Duration.ofSeconds(1))
                        .retryMax(3))

                    val retryWhen2 = getSpeciesStream().retryWhen(Retry
                        .any<Throwable>()
                        .fixedBackoff(Duration.ofSeconds(1))
                        .timeout(Duration.ofSeconds(10)))

                    // will retry 3 times (with delay of 1, 2 and 3 seconds between retries) and will not report any
                    // error
                    val retryWhen3 = getSpeciesStream().retryWhen { errors ->
                        errors
                            .zipWith(Flux.range(1, 3), BiFunction<Throwable, Int, Int> { n, i -> i })
                            .flatMap { retryCount ->
                                println("delay retry by $retryCount second(s)")
                                Flux.interval(Duration.ofSeconds(retryCount.toLong())).take(1)
                            }
                    }

                    // will retry 5 times (with delay of 1, 2, 3... seconds between retries) and will report the
                    // last error from the getSpeciesStream flux. But retry is not reset, so the second error will
                    // continue "when" with the next retryCount after the recovery from the first one
                    val retryWhen4 = getSpeciesStream().retryWhen { errors -> errors
                        .zipWith(Flux.range(1, 6))
                        .flatMap { tuple2 ->
                            val error = tuple2.t1
                            val retryCount = tuple2.t2
                            if (retryCount >= 6) {
                                return@flatMap Mono.error<Throwable>(error)
                            }
                            println("delay retry by $retryCount second(s)")
                            Mono.delay(Duration.ofSeconds(retryCount.toLong()))
                        }
                    }

                    // This retry will reset the counter
                    val maxRetries = 5;
                    val retryCount = AtomicInteger(0)

                    val retryWhen5 = getSpeciesStream()
                        .doOnNext { retryCount.lazySet(0) }
                        .retryWhen { errors -> errors
                        .flatMap { error ->
                            val retry = retryCount.incrementAndGet()
                            if (retry > maxRetries) {
                                return@flatMap Mono.error<Throwable>(error)
                            }
                            println("delay retry by $retry second(s)")
                            Mono.delay(Duration.ofSeconds(retryCount.toLong()))
                        }
                    }

                    // same as before but with a helper function
                    val retryWhen6 = getSpeciesStream().compose(retryWithReset(5))

                    ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_STREAM_JSON)
                        .body(retryWhen6, Species::class.java)
                })
    }

    fun <T> retryWithReset(maxRetries: Int): java.util.function.Function<Flux<T>, Flux<T>> =
        java.util.function.Function { flux: Flux<T> ->
            val retryCount = AtomicInteger(0)
            flux
                .doOnNext { retryCount.lazySet(0) }
                .retryWhen { errors -> errors
                    .flatMap { error ->
                        val retry = retryCount.incrementAndGet()
                        if (retry > maxRetries) {
                            return@flatMap Mono.error<Throwable>(error)
                        }
                        println("delay retry by $retry second(s)")
                        Mono.delay(Duration.ofSeconds(retryCount.toLong()))
                    }
                }
        }

}