package com.example.reservationserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.r2dbc.ConnectionFactoryAutoConfiguration
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
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
import java.time.Duration


data class Reservation(var name: String) {
    @Id var id: Int? = null
}

interface ReservationRepository : ReactiveCrudRepository<Reservation, Int>

@Import(ConnectionFactoryAutoConfiguration::class)
@EnableR2dbcRepositories
@SpringBootApplication
class ReservationServer {

    @Configuration
    class Initializer(val reservationRepository: ReservationRepository,
                      val databaseClient: DatabaseClient ) {

        @EventListener(ApplicationReadyEvent::class)
        fun init() {
            // schema.sql is not working for some reason...
            val createDbFlux = Flux.just(
                "CREATE TABLE IF NOT EXISTS reservation (id SERIAL PRIMARY KEY, name VARCHAR NOT NULL);")
                .flatMap {
                    databaseClient.execute()
                        .sql(it)
                        .fetch()
                        .rowsUpdated()
                }

            val savedReservationsFlux = Flux.just("Andy", "Mr. White", "Ingvar", "Joan", "Starbuxman", "Joshua")
                .map { Reservation(it) }
                .flatMap { reservationRepository.save(it) }

            createDbFlux
                .then(reservationRepository.deleteAll())
                .thenMany(savedReservationsFlux)
                .subscribe()
        }
    }

    @Configuration
    class WebConfiguration(val reservationRepository: ReservationRepository) {

        @Bean
        fun routes(): RouterFunction<ServerResponse> = RouterFunctions
            .route(
                RequestPredicates.GET("/reservations"),
                HandlerFunction { ServerResponse.ok().body(reservationRepository.findAll(), Reservation::class.java) }
            )
            .andRoute(RequestPredicates.GET("/reservation-stream"),
                HandlerFunction {
                    val infiniteProducer = reservationRepository.findAll().repeat()
                    ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_STREAM_JSON)
                        .body(Flux
                            .zip(Flux.interval(Duration.ofSeconds(1)), infiniteProducer)
                            .map { it.getT2() },
                            Reservation::class.java)
                }
            )
    }

    /**
     * In case spring boot autoconfiguration is not used (i.e. no spring-boot-starter-data-r2dbc dependency), we can
     * configure the r2dbc connection factory like specified below
     */

//    @Bean
//    fun repository(factory: R2dbcRepositoryFactory): ReservationRepository {
//        return factory.getRepository(ReservationRepository::class.java)
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
    runApplication<ReservationServer>(*args)
}
