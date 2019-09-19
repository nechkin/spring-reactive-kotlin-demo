package com.example.server

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class SpeciesService(val speciesRepository: SpeciesRepository) {

    fun create(speciesDto: Mono<SpeciesDto>): Flux<Species> {
        val speciesMono = speciesDto.map { Species(it.name) }
        // or speciesMono.doOnNext {speciesRepository.save(it)}
        return speciesRepository.saveAll(speciesMono)
    }

    fun read(id: Mono<Int>): Mono<Species> {
        return speciesRepository.findById(id)
    }

    fun update(id: Mono<Int>, speciesDto: Mono<SpeciesDto>): Mono<Species> {
        return speciesRepository.findById(id)
            .defaultIfEmpty(Species(""))
            .flatMap { species ->
                speciesDto.map {
                    species.name = it.name
                    species
                }
            }
            .flatMap {speciesRepository.save(it)}
    }

    fun delete(id: Mono<Int>): Mono<Void> {
        return speciesRepository.deleteById(id)
    }
}