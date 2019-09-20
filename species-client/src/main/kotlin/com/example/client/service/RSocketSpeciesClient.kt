package com.example.client.service

import com.example.client.Species
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class RSocketSpeciesClient(val rSocketRequester: RSocketRequester): SpeciesClient {
    override fun getSpecies(): Flux<Species> {
        return rSocketRequester
            .route("getSpeciesById")
            .data(0)
            .retrieveFlux(Species::class.java)
    }

    override fun getSpeciesStream(): Flux<Species> {
        return rSocketRequester
            .route("feedSpecies")
            .data(0)
            .retrieveFlux(Species::class.java)    }

    fun updateSpeciesAsync(species: Species): Mono<Void> {
        return rSocketRequester
            .route("updateSpeciesIfExists")
            .data(species)
            .send()
    }
}