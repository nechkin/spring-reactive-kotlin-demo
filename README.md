# Spring Boot Kotlin reactive sample

A sample application developed with Kotlin to try out some of Spring's support for reactive programming: WebFlux in 
general, Router functions (instead of ordinary Handler Mappings), reactive WebClient, R2DBC (with Postgres connector), 
Spring Gateway (as a substitution for Zuul).

## Run it

`./gradlew assemble --parallel`

Reactive server

`./gradlew reservation-server:bootRun`

Servlet application (to compare latencies)

`./gradlew server-mvc:bootRun`

Reactive gateway application

`./gradlew reservation-client:bootRun`

## Docker

Postgres Database:

`docker run -d --name postgres-reactive-demo -p 5432:5432 -e POSTGRES_USER=dbuser -e POSTGRES_PASSWORD=dbpass -e POSTGRES_DB=reactive postgres`

Redis (for rate limiter to work):

`docker run -d --name redis-reactive-demo -p 6379:6379 redis:5.0.5`

## CRUD

* List all: curl localhost:8080/species
* Create: curl -XPOST -H "Content-Type: application/json" -d '{"name":"Test"}' localhost:8080/species
* Read: curl localhost:8080/species/5
* Update: curl -XPUT -H "Content-Type: application/json" -d '{"name":"Test2"}' localhost:8080/species/5
* Delete: curl -XDELETE localhost:8080/species/5

## Using gateway application

reservation-client - the gateway app with basic security (single user is defined with login "user" and password "pass")

**Dynamic routing**

See SpeciesClientApplication#routeLocator.

curl -v -H"X-CUSTOM-HEADER: value" localhost:8081/unstable-proxy

/unstable-proxy is dynamically routed, it has a 50% change to succeed or fail with 404.

**Rate limiter url**

See SpeciesClientApplication#redisRateLimiter

siege -H"Authorization: Basic dXNlcjpwYXNz" -c 1 -r 100 http://localhost:8081/proxy

**Http endpoints that uses WebClient to call SpeciesServer**

See SpeciesRoutesConfiguration#speciesRoutes, WebSpeciesClient

curl localhost:8081/species-stream/names

curl localhost:8081/species-stream

**RSocket**

See SpeciesRSocketController, ManualRSocketSpeciesClient, RSocketSpeciesClient

* With manually curated RSocket:

curl localhost:8081/rs/manual/species

curl localhost:8081/rs/manual/species-stream/names

* With Spring RSocketRequester:

curl localhost:8081/rs/species

curl localhost:8081/rs/species-stream/names

curl localhost:8081/rs/species/update/87?name=ASDASD - fire and forget, request is sent to the species-server, no 
response is awaited

## Other points of interest

* species-client: SpeciesRoutesConfiguration#speciesRoutes - Using retry to continue reading a stream after a connection problem.
 Try restarting the species-service when `curl localhost:8081/species-stream` is active 
 
* species-client: ManualRSocketSpeciesClient - using RScoket directly to communicate with the server

* species-client: RSocketSpeciesClient - using Spring RSocket support to communicate with the server

* species-service: ManualRSocketServer - configure RSocket to accept connections manually

* species-service: application.yml - using spring.rsocket.sercer.port to specify the port for Spring autoconfigured 
RSocket server 

* `apply plugin: "org.jetbrains.kotlin.plugin.spring"` is used, see parent build.gradle, so to avoid adding open
 modifier for Spring proxied classes                  

## Testing latency

The [Siege](https://github.com/JoeDog/siege) benchmarking utility can be used to test some of the apps endpoint:

Reactive version has the default number of Event Loop threads for Netty to server the request, which is equal to the 
number of processors (can see them in jconsole by the name like reactor-http-epoll-#), and a pool of 10 threads that 
block, and are used to avoid blocking the Event Loop threads.

Servlet version has number of Tomcat threads configured to 10.
 
-b - no delyas between requests
-c - number of concurrent users
-r - number of requests per user

Benchmark reactive server:
siege -b -c 20 -r 10 http://localhost:8080/sse-generate
siege -b -c 20 -r 10 http://localhost:8080/json-stream
siege -b -c 20 -r 10 http://localhost:8080/sse-publisher

Benchmark servlet version:
siege -b -c 20 -r 10 http://localhost:8080/array

Transactions per second rate should be similar and close to 10 trans/sec 

**Some other things used when playing around with the code:**

To see the number of open file descriptors

> jps

> lsof -p <process id> | wc -l

To monitor how threads are doing

> `jconsole`

## Some of the technologies used

* Spring Boot
* Gradle
* Spring WebFlux
* R2DBC
* Spring Cloud Gateway
* RSocket

## TODO

* try backpressure with rsocket
* unit tests, something like this for webClient:
```
Flux<Species> flux = webTestClient
    .get()
    .uri("/conrollerPath")
    .accept(MediaType.TEXT_EVENT_STREAM)
    .exchange()
    .expectStatus()
    .isOk()
    .returnResult(Species.class)
    .getResponseBody();
StepVerifier.create(flux)
    .expectNextCount(?)
    .thenCancel()
    .verify();
```
* two-way negotiation via RSocket
