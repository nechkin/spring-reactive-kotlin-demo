package com.example.server.controller

import com.example.server.SpeciesRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.rsocket.AbstractRSocket
import io.rsocket.ConnectionSetupPayload
import io.rsocket.Payload
import io.rsocket.RSocket
import io.rsocket.RSocketFactory
import io.rsocket.SocketAcceptor
import io.rsocket.transport.netty.server.TcpServerTransport
import io.rsocket.util.DefaultPayload
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.stream.Collectors

/**
 * Manual RSocket server configuration with a couple of listening tcp rsockets serving species data
 */
@Configuration
class ManualRSocketServer(val speciesRepository: SpeciesRepository,
                          val objectMapper: ObjectMapper) {

    private val tcpServerTransport = TcpServerTransport.create(7070)

    @EventListener(ApplicationReadyEvent::class)
    fun manualRsocketServer() {

        val speciesRSocket: Mono<RSocket> = Mono.just(object : AbstractRSocket() {
            override fun requestResponse(payload: Payload?): Mono<Payload> {
                return speciesRepository.findAll()
                    .collect(Collectors.toList())
                    .map(objectMapper::writeValueAsString)
                    .map(DefaultPayload::create)
            }

            override fun requestStream(payload: Payload?): Flux<Payload> {
                return speciesRepository.findAll()
                    .repeat()
                    .zipWith(Flux.interval(Duration.ofSeconds(1)))
                    .map { it.t1 }
                    .map(objectMapper::writeValueAsString)
                    .map(DefaultPayload::create)
            }
        })

        val socketAcceptor = SocketAcceptor { _: ConnectionSetupPayload, _: RSocket ->
            speciesRSocket
        }

        RSocketFactory
            .receive()
            .acceptor(socketAcceptor)
            .transport(tcpServerTransport)
            .start()
            .onTerminateDetach()
            .subscribe()
    }
}