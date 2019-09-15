package com.example.reservationserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.data.annotation.Id
import org.springframework.data.r2dbc.core.DatabaseClient
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicates
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

data class Species(var name: String) {
    @Id var id: Int? = null
}

data class SpeciesDto(val name: String)

interface SpeciesRepository : ReactiveCrudRepository<Species, Int>

@EnableR2dbcRepositories
@SpringBootApplication
class SpeciesServiceApplication {

    @Configuration
    class Initializer(val speciesRepository: SpeciesRepository,
                      val databaseClient: DatabaseClient ) {

        @EventListener(ApplicationReadyEvent::class)
        fun init() {
            // schema.sql is not working for r2dbc, though it should accodring to the docs:
            // https://github.com/spring-projects-experimental/spring-boot-r2dbc/blob/master/documentation.adoc#initialize-a-database-using-r2dbc
            val createDbFlux = Flux.just(
                "CREATE TABLE IF NOT EXISTS species (id SERIAL PRIMARY KEY, name VARCHAR NOT NULL);")
                .flatMap {
                    databaseClient.execute()
                        .sql(it)
                        .fetch()
                        .rowsUpdated()
                }

            val savedReservationsFlux = Flux.just("Mus Musculus", "Rattus norvegicus",
                "Homo sapiens", "Gallus gallus")
                .map { Species(it) }
                .flatMap { speciesRepository.save(it) }

            createDbFlux
                .then(speciesRepository.deleteAll())
                .thenMany(savedReservationsFlux)
                .subscribe()
        }
    }

    @Configuration
    class WebConfiguration(val speciesRepository: SpeciesRepository) {

        @Bean
        fun routes(): RouterFunction<ServerResponse> = RouterFunctions

            .route(
                RequestPredicates.GET("/species"),
                HandlerFunction { ServerResponse.ok().body(speciesRepository.findAll(), Species::class.java) }
            )

            .andRoute(RequestPredicates.GET("/species-stream"),
                HandlerFunction {
                    val infiniteProducer = speciesRepository.findAll().repeat()
                    ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_STREAM_JSON)
                        .body(Flux
                            .zip(Flux.interval(Duration.ofSeconds(1)), infiniteProducer)
                            .map { it.getT2() },
                            Species::class.java)
                }
            )

            .andRoute(RequestPredicates.GET("/species/{id}"),
                HandlerFunction { request ->
                    val speciesMono = Mono.justOrEmpty(request.pathVariable("id"))
                        .map(Integer::valueOf)
                        .flatMap(speciesRepository::findById)
                    ServerResponse.ok().body(speciesMono, Species::class.java)
                }
            )

            .andRoute(RequestPredicates.POST("/species"),
                HandlerFunction { request ->
                    val speciesMono = request.bodyToMono(SpeciesDto::class.java)
                        .map { Species(it.name) }
                    val savedSpeciesMono = speciesRepository.saveAll(speciesMono)
                    // or speciesMono.doOnNext {speciesRepository.save(it)}
                    ServerResponse.ok().body(savedSpeciesMono, Species::class.java)
                })

            .andRoute(RequestPredicates.PUT("/species/{id}"),
                HandlerFunction { request ->
                    val modifiedOrNewEntityMono = Mono.justOrEmpty(request.pathVariable("id"))
                        .map(Integer::valueOf)
                        .flatMap(speciesRepository::findById)
                        .defaultIfEmpty(Species(""))
                        .flatMap { species ->
                            request.bodyToMono(SpeciesDto::class.java)
                                .map {
                                    species.name = it.name
                                species
                                }
                        }

                    ServerResponse.ok().body(speciesRepository.saveAll(modifiedOrNewEntityMono),
                        Species::class.java)
                })
    }

    /**
     * In case spring boot autoconfiguration is not used (i.e. no spring-boot-starter-data-r2dbc dependency), we can
     * configure the r2dbc connection factory like specified below
     */

//    @Bean
//    fun repository(factory: R2dbcRepositoryFactory): SpeciesRepository {
//        return factory.getRepository(SpeciesRepository::class.java)
//    }
//    @Bean
//    fun factory(client: DatabaseClient): R2dbcRepositoryFactory {
//        return R2dbcRepositoryFactory(client, reactiveDataAccessStrategy())
//    }
//
//    @Bean
//    fun reactiveDataAccessStrategy(): ReactiveDataAccessStrategy =
//        DefaultReactiveDataAccessStrategy(PostgresDialect.INSTANCE)
//
//    @Bean
//    fun databaseClient(factory: ConnectionFactory): DatabaseClient {
//        return DatabaseClient.builder().connectionFactory(factory).build()
//    }
//    @Bean
//    fun connectionFactory(): PostgresqlConnectionFactory {
//        val config = PostgresqlConnectionConfiguration.builder()
//            .host("localhost")
//            .port(5432)
//            .database("dbuser")
//            .username("dbpass")
//            .password("reactive")
//            .build()
//        return PostgresqlConnectionFactory(config)
//    }
}

fun main(args: Array<String>) {
    runApplication<SpeciesServiceApplication>(*args)
}
