package com.example.client.service

import com.example.client.Species
import reactor.core.publisher.Flux

interface SpeciesClient {
    fun getSpecies(): Flux<Species>
    fun getSpeciesStream(): Flux<Species>
}