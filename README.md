# Spring boot reactive demo sample

A sample application developed in Kotlin to try out some of Spring's support for reactive programming: WebFlux, 
Router functions (instead of ordinary Handler Mappings), R2DBC reactive relational repository (with Postgres).

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

## Using gateway application

### Proxy for reservation-server

reservation-client - the gateway app with basic security (single user set up with login "user" and password "pass")

curl -v -uuser:pass -H"X-CUSTOM-HEADER: value" localhost:8081/unstable-proxy

/unstable-proxy is dynamically routed, it has a 50% change to succeed or fail with 404.

## Some of the technologies used

* Spring Boot
* Gradle
* Spring WebFlux
* R2DBC
* Spring cloud gateway
