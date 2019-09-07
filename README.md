# Spring boot reactive demo sample

A sample application developed in Kotlin to try out some of Spring's support for reactive programming.

## Run it

`./gradlew reservation-server:bootRun`

An MVC application (to compare latencies)

`./gradlew server-mvc:bootRun`

## Docker

Postgre Database:

`docker run -d --name postgres-reactive-demo -p 5432:5432 -e POSTGRES_USER=dbuser -e POSTGRES_PASSWORD=dbpass -e POSTGRES_DB=reactive postgres`

## Testing latency

The [Siege](https://github.com/JoeDog/siege) benchmarking utility can be used to test some of the apps endpoint:

siege -b -c 20 -r 10 http://localhost:8080/sse-generate
siege -b -c 20 -r 10 http://localhost:8080/sse-generate
siege -b -c 20 -r 10 http://localhost:8080/sse-generate

**Some other things used when playing around with the code:**

To see the number of open file descriptors

> jps

> lsof -p <process id> | wc -l

To monitor how threads are doing

> `jconsole`

## Technologies used

* Spring Boot
* Gradle
* Spring WebFlux
* 
