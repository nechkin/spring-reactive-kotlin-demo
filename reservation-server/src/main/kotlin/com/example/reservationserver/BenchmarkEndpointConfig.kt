package com.example.reservationserver

import org.reactivestreams.Publisher
import org.reactivestreams.Subscription
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicates
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.SynchronousSink
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@Configuration
class BenchmarkEndpointConfig {
    // run fetchResponseValues on a worker thread provided by the scheduler(), not to block netty's event loop thread
    @Bean
    fun scheduler() = Schedulers.fromExecutor(Executors.newScheduledThreadPool(10))

    @Bean
    fun routesLatencyExperiments(): RouterFunction<ServerResponse> {
        return RouterFunctions
            .route(
                RequestPredicates.GET("/sse-generate"),
                HandlerFunction {
                    var count = 0;
                    val generated = Flux.generate(
                        { "response" },
                        { message, sink: SynchronousSink<String> ->
                            TimeUnit.MILLISECONDS.sleep(500)
                            sink.next(message + " " + count)
                            if (count == 1) {
                                sink.complete()
                                return@generate ""
                            }
                            count++
                            "another"
                        })
                        .subscribeOn(scheduler())
                        .log()

                    ServerResponse
                        .ok()
                        .contentType(MediaType.TEXT_EVENT_STREAM)
                        .body(generated)
                }
            )
            .andRoute(
                RequestPredicates.GET("/json-stream"),
                HandlerFunction {
                    ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_STREAM_JSON)
                        .body(
                            Flux.interval(Duration.ofMillis(500))
                                .takeWhile({ it < 2 })
                                .map { mapOf(Pair("value", "response " + it)) }
                                .subscribeOn(scheduler())
                        )
                }
            )
            .andRoute(
                RequestPredicates.GET("/sse-publisher"),
                HandlerFunction {
                    ServerResponse
                        .ok()
                        .contentType(MediaType.TEXT_EVENT_STREAM)
                        .body(Publisher<String> { subscriber ->
                            subscriber.onSubscribe(object : Subscription {
                                var count = AtomicInteger()
                                override fun request(n: Long) {
                                    val prev = count.addAndGet(1)
                                    if (prev > 2) {
                                        return
                                    }
                                    Thread.sleep(500)
                                    subscriber.onNext("response " + prev)
                                    if (count.get() >= 2) {
                                        subscriber.onComplete()
                                    }
                                }

                                override fun cancel() {
                                    count.set(3);
                                }
                            })
                        })
                }
            )
    }

    /**
     * Can use regular controller without router functions
     */
    @RestController
    class MoreRoutes(val scheduler: Scheduler) {

        @GetMapping("/blocking")
        fun blocking(): Flux<String> {
            // Blocking Netty's event loop thread with sleep here
            TimeUnit.SECONDS.sleep(1)
            return Flux.fromArray(arrayOf("response", "another"))
        }

        @GetMapping("/blockingMono")
        fun blockingMono(): Mono<String> {
            // Blocking Netty's event loop thread with sleep here
            return Mono.fromCallable {
                TimeUnit.SECONDS.sleep(1)
                "blocking response"
            }
        }

        @GetMapping("/reactiveMono")
        fun reactiveMono(): Mono<String> {
            return Mono.fromCallable {
                TimeUnit.SECONDS.sleep(1)
                "blocking response"
            }.subscribeOn(scheduler)
        }

    }

//    companion object {
//        private fun fetchResponseValues(): Array<String> {
//            TimeUnit.SECONDS.sleep(1)
//            return arrayOf()
//        }
//    }
}
