package com.example.client.service

import com.example.client.Species
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.rsocket.RSocket
import io.rsocket.RSocketFactory
import io.rsocket.transport.netty.client.TcpClientTransport
import io.rsocket.util.DefaultPayload
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class ManualRSocketSpeciesClient(private val tcpClientTransport: TcpClientTransport,
                                 private val objectMapper: ObjectMapper)
    : SpeciesClient {

    override fun getSpecies(): Flux<Species> {
        return RSocketFactory
            .connect()
            .transport(tcpClientTransport)
            .start()
            .flatMapMany { socket: RSocket ->
                socket
                    .requestResponse(DefaultPayload.create(ByteArray(0)))
                    .map { payload -> payload.dataUtf8 }
                    .flatMapMany { json ->
                        // without jackson kotlin module
                        // val speciesList: List<Species> = objectMapper.readValue(json, object : TypeReference<List<Species>>() {})
                        val speciesList: List<Species> = objectMapper.readValue(json)
                        Flux.fromIterable(speciesList)
                    }
            }
    }

    override fun getSpeciesStream(): Flux<Species> {
        return RSocketFactory
            .connect()
            .transport(tcpClientTransport)
            .start()
            .flatMapMany { socket: RSocket ->
                socket
                    .requestStream(DefaultPayload.create(ByteArray(0)))
                    .map { payload -> payload.dataUtf8 }
                    .map { json -> objectMapper.readValue(json, Species::class.java) }
                    .doFinally { signal -> socket.dispose() }
            }

    }
}