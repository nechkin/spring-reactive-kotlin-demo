package com.example.client.controller

import com.example.client.Species
import com.example.client.service.ManualRSocketSpeciesClient
import com.example.client.service.RSocketSpeciesClient
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


/**
 * Instead of using RouterFunctions, using regular annotated controller with WebFlux.
 * Here we are using RSocket to fetch data from the "species-service"
 */
@RequestMapping("/rs")
@RestController
class SpeciesRSocketController(val manualRSocketSpeciesClient: ManualRSocketSpeciesClient,
                               val rSocketSpeciesClient: RSocketSpeciesClient) {

    @GetMapping("/manual/species", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun speciesListManual(): Flux<Species> = manualRSocketSpeciesClient.getSpecies()

    @GetMapping("/manual/species-stream/names", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun speciesNamesStreamManual(): Flux<String> = manualRSocketSpeciesClient.getSpeciesStream().map(Species::name)

    @GetMapping("/species", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun speciesList(): Flux<Species> = rSocketSpeciesClient.getSpecies()

    @GetMapping("/species-stream/names", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun speciesNamesStream(): Flux<String> = rSocketSpeciesClient.getSpeciesStream().map(Species::name)

    @GetMapping("/species/update/{id}")
    fun speciesUpdate(@PathVariable("id") id: Int,
                      @RequestParam("name") name: String): Mono<Void> {
        return rSocketSpeciesClient.updateSpeciesAsync(Species(id, name))
    }
}