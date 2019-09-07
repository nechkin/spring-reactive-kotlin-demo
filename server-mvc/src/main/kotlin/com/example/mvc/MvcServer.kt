package com.example.mvc

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.async.DeferredResult
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Supplier


@SpringBootApplication
class MvcServer {

    @RestController
    class MoreRoutes {
        @GetMapping(path = ["/scalar"], produces = [MediaType.APPLICATION_JSON_VALUE])
        internal fun scalar(): String {
            TimeUnit.SECONDS.sleep(1)
            return "response"
        }

        @GetMapping(path = ["/array"], produces = [MediaType.APPLICATION_JSON_VALUE])
        internal fun array(): List<String> {
            TimeUnit.SECONDS.sleep(1)
            return listOf("response", "another")
        }

        @Bean
        fun configurer() = object : WebMvcConfigurer {
            override fun configureAsyncSupport(configurer: AsyncSupportConfigurer?) {
                val t = ThreadPoolTaskExecutor()
                t.corePoolSize = 10
                t.maxPoolSize = 100
                t.setQueueCapacity(50)
                t.setAllowCoreThreadTimeOut(true)
                t.keepAliveSeconds = 120
                t.initialize()
                configurer!!.setTaskExecutor(t)
            }
        }

        private val threadPool = Executors.newCachedThreadPool()

        @GetMapping(path = ["/scalarNonBlocking"], produces = [MediaType.APPLICATION_JSON_VALUE])
        internal fun scalarNoBlocking(): DeferredResult<String> {
            val deferredResult = DeferredResult<String>()

            CompletableFuture.supplyAsync<String>(
                Supplier<String> {
                    TimeUnit.SECONDS.sleep(1)
                    "response"
                },
                threadPool)
                .whenCompleteAsync { result, throwable -> deferredResult.setResult(result) }

            return deferredResult
        }

        @GetMapping("/stream")
        fun stream(): ResponseEntity<SseEmitter> {

            val emitter = SseEmitter()
            threadPool.execute {
                generateSequence(0, {it + 1}).forEach {
                    try {
                        TimeUnit.SECONDS.sleep(1)
                        emitter.send("$it", MediaType.TEXT_PLAIN);
                    } catch (e: Exception) {
                        emitter.completeWithError(e)
                        return@execute
                    }

                }
                emitter.complete()
            }

            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(emitter)
        }
    }
}

fun main(args: Array<String>) {
    runApplication<MvcServer>(*args)
}
