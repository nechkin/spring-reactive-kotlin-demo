package com.example.server.controller

import com.example.server.Species
import com.example.server.SpeciesRepository
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration


@Controller
class RSocketController(val speciesRepository: SpeciesRepository) {

    @MessageExceptionHandler
    fun handleException(e: Exception): Mono<String> {
        return Mono.just(e.message ?: "unknown error")
    }

    // Request/response
    @MessageMapping("getSpeciesById")
    fun getSpeciesById(): Flux<Species> {
        return speciesRepository.findAll()
    }

    // Fire and forget
    @MessageMapping("updateSpeciesIfExists")
    fun updateSpeciesIfExists(species: Species): Mono<Void> {
        if (species.id == null) {
            return Mono.empty()
        }
        return speciesRepository.findById(species.id!!)
            .doOnNext { it.name = species.name }
            .flatMap {speciesRepository.save(it)}
            .thenEmpty(Mono.empty())
    }

    // Stream
    @MessageMapping("feedSpecies")
    fun feedSpecies(): Flux<Species> {
        return speciesRepository.findAll()
            .repeat()
            .zipWith(Flux.interval(Duration.ofSeconds(1)))
            .map { it.t1 }
    }
}