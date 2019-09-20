package com.example.client.service

import com.example.client.Species
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux

@Service
class WebSpeciesClient(val webClient: WebClient): SpeciesClient {

    override fun getSpecies(): Flux<Species> {
        return this.webClient
            .get()
            .uri("http://localhost:8080/species")
            .retrieve()
            .bodyToFlux(Species::class.java)
    }

    override fun getSpeciesStream(): Flux<Species> {
        return this.webClient
            .get()
            .uri("http://localhost:8080/species-stream")
            .accept(MediaType.APPLICATION_STREAM_JSON)
            .exchange()
            .flatMapMany { clientResponse -> clientResponse.bodyToFlux(Species::class.java) }
            .log()
    }
}