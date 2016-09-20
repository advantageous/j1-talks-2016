

# A fuller example using Kafka, multiple services, Promises.all



## Step 1 implement the store operation in SubscriptionRepository

Add the `store` operation in the `SubscriptionRepository` class.

#### ACTION Edit the file ./src/main/java/io/advantageous/reakt/examples/repository/SubscriptionRepository and finish store method
```java
package io.advantageous.reakt.examples.repository;

...

public class SubscriptionRepository {
    private final CassandraTemplate<Subscription> cassandraTemplate;

    ...

    public SubscriptionRepository(final int replicationFactor, final List<URI> cassandraUris) {
        cassandraTemplate = new CassandraTemplate<>(replicationFactor,
                                                    cassandraUris, TABLE_DEFINITION, KEYSPACE);
    }

    public Promise<Boolean> store(Subscription subscription){
        return invokablePromise(promise -> {

                }
            );
    });
    }
```

The method `Promise.invokablePromise` returns an *invokeable promise*, which is a handle on an async operation
call. The client code can register error handlers and async return handlers (callbacks) for the async operation
and then `invoke` the async operation.  

When you return a promise, client code can call your method as follows:
#### INFO Calling this REPO from a service
```java

          mgmt.increment(MGMT_CREATE_KEY);
          repository.store(subscription)
                    .then(result -> {
                        logger.info("subscription created id");
                        promise.resolve(result);
                    })
                    .catchError(error -> {
                        logger.error("Unable to create subscription", error);
                        promise.reject("Unable to create subscription");
                    })
                    .invoke();
           
```


Notice you have different handlers for handling the successful outcome (`then`) versus the unsuccessful outcome (`catchError`).

### Background on promise handlers

Here are the different types of promises handlers.

* `then` - use this to handle async calls (success path)
* `catchError` - use this to handle async calls (error path)
* `thenExpected` - use this to handle async calls whose result could be null
* `thenSafe` - use this to report errors with async call and your handler
* `thenSafeExpected` - same as `thenSafe` but used where the result could be null
* `thenMap` - converts one type of promise into another type of promise

The handlers `thenExpect` and `thenSafeExpect` return a Reakt `Expected` instance. 
`Expected` is like `Option` in Java 8, it has methods like `map`, `filter`, etc. 
and adds methods `ifEmpty`, `isEmpty`. This gives a nice fluent API when you don't 
know if a successful return is null or not. 

The methods `then` and `thenSafe` async return the result that is not wrapped in an
`Expected` object, i.e., the raw result. Use `then` and `thenSafe` when you 
know the async return will not be null. Use `thenExpect` and `thenSafeExpect`
if the value could be null or if you want to `map` or `filter` the result. 

Use `thenMap` when a promise returns for example a `List<Employee>`, but you only 
want the first `Employee`. See [`Promise.thenMap`](https://github.com/advantageous/reakt/wiki/Promise.thenMap) for more details.

Note unless you are using a reactor, custom Promises or blocking promises, the `then*` handlers
will typically run in a foreign thread and if they throw an exception depending on the library,
they could get logged in an odd way. If you think your handler could throw an exception (not the 
service you are calling but your handlers), then you might want to use `thenSafe` or 
`thenSafeExpect`. These will wrap your async `then*` handler code in a `try/catch` and pass the thrown 
exception to a `ThenHandlerException` to `catchError`. If your code ever hangs when making an async call,
try using a `thenSafe` or `thenSafeExpect`. They ensure that any exceptions thrown in your handler don't
get dropped by the system you are using, which could indicate a lack of understanding of the async lib 
you are using or that you are using it wrong. If it hangs, try `thenSafe` or `thenSafeExpect`. They 
help you debug async problems. 



## Step 2 finish the ifConnected method

Next we need to finish up the `ifConnected` operation

#### ACTION Edit the file ./src/main/java/io/advantageous/reakt/examples/template/CassandraTemplate and finish ifConnected method
```java

    private boolean isConnected() {
        return sessionRef.get() != null && !sessionRef.get().isClosed();
    }

    private void ifConnected(final String operation,
                            final Promise<?> promise, final Runnable runnable) {
        // If we are not connected, try to connect, but fail this request.
        if (!isConnected()) {
            forceConnect();
            //Promise rejected because we were not connected.
            promise.reject("Not connected to cassandra for operation " + operation);
        } else {
            // Try running the operation
            try {
                runnable.run();
            } catch (Exception ex) {
                //Operation failed, exit
                promise.reject("Error running " + operation, ex);
            }
        }
    }

```

Notice that we catch the `Exception` and then call `promise.reject` to send the exception back to the handler.
We also implement a fail fast operation if we are not yet connected of lost our connection (outage?). The fail fast operation attempts a reconnect.

## Step 3 Finish the insert method

#### ACTION Edit the file ./src/main/java/io/advantageous/reakt/examples/template/CassandraTemplate and finish insert method
```java
    public void insert(Promise<Boolean> promise, Insert insert){
            registerCallback(sessionRef.get().executeAsync(insert),
                    promise(ResultSet.class)
                            .catchError(promise::reject)
                            .then(resultSet -> promise.resolve(resultSet.wasApplied()))
            );
        }
```

The `registerCallback` method is from the [Guava integration with Reakt](http://advantageous.github.io/reakt-guava/). Cassandra uses [Guava](https://github.com/google/guava) as do many other libs for their async lib operations. 

## Step 4 Finish the create method in the service impl

#### ACTION Edit the file ./src/main/java/io/advantageous/reakt/examples/service/SubscriptionServiceImpl and finish create method
```java
... 

@RequestMapping("/subscription-service")
public class SubscriptionServiceImpl implements SubscriptionService {
    private static final String PATH = "/subscription";
    
    ...
    @Override
    @POST(value = PATH)
    public Promise<Boolean> create(final Subscription subscription) {
        return invokablePromise(promise -> {
            mgmt.increment(MGMT_CREATE_KEY);

            ThirdPartySubscriptionService.create(subscription)
                    .then(thirdPartyId -> {
                        logger.info(" subscription created id="+thirdPartyId);


                        subscription.setThirdPartyId(thirdPartyId);
                        
                        
                        repository.store(subscription)
                                .then(result -> {
                                    logger.info("subscription created id");
                                    promise.resolve(result);
                                })
                                .catchError(error -> {
                                    logger.error("Unable to create subscription", error);
                                    promise.reject("Unable to create subscription");
                                })
                                .invoke();
                        
                    })
                    .catchError(error -> {
                        logger.error("Unable to create stripe subscription", error);
                        promise.reject("Unable to create stripe subscription");
                    }).invoke();
            
        });
    }
```

A promise has to be resolved (`promise.resolve`) or rejected (`promise.reject`). 

Once you are done editing the files, you can test them.
There is a `SubscriptionRepoTest`.

## Step 5 Run the test

#### src/test/java/io/advantageous/reakt/examples/repository/SubscriptionRepoTest
```java

@Category(DockerTest.class)
public class SubscriptionRepoTest {
    private SubscriptionRepository repository;
    private Subscription subscription;

    @Before
    public void before() throws Exception {
        String id = UUID.randomUUID().toString();
        String thirdPartyId = UUID.randomUUID().toString();
        String name = "test subscription";
        long createTime = System.currentTimeMillis();

        subscription = new Subscription(id, name, thirdPartyId, createTime);


        repository = new SubscriptionRepository(1, ConfigUtils.getConfig("subscription")
                                                              .getConfig("cassandra")
                                                              .getUriList("uris"));

        repository.connect().invokeAsBlockingPromise().get();
        
    }

    @After
    public void after() throws Exception {
        repository.close();
    }

    @Test
    public void testStore() throws Exception {
        final Promise<Boolean> promise = repository.store(subscription)
                                                   .invokeAsBlockingPromise();

        assertTrue(promise.success());
        assertTrue(promise.get());
    }
    ...
```


Notice the above uses a `BlockingPromise`. 
A `BlockingPromise` is very much like a Java Future. It is blocking. This is useful for unit testing and for legacy integration.

The method `invokeAsBlockingPromise` has a version that takes a timeout duration so your tests do not hang forever if there is an error. The `invokeAsBlockingPromise` greatly simplifies testing of async software which can be a bit difficult. 


To run the test, the docker containers have to be running.

You can control the docker containers from gradle.
* dockerTest - Run docker integration tests (works with tests that have `@Category(DockerTest.class)`
* showDockerContainers
* stopTestDocker - Stop docker containers used in tests
* startTestDocker - Start docker containers used in tests

If you want to run the examples in the IDE, just run this once
```
$ docker startTestDocker
```

Then use the IDE to run the unit test.

## Step 6 Test create using REST interface

#### ACTION Run the app
```
$ gradle clean build run
```

The above should run the application and bind the service port to 8081 and the admin port to 9090.

#### ACTION Add a Subscription
```
 $ curl -X POST http://localhost:8082/v1/subscription-service/subscription \
  -d '{"name":"test", "thirdPartyId":"1234"}' -H "Content-type: application/json" | jq .
```

The above uses curl to POST JSON Subscription to our example.

#### ACTION Read Subscriptions
```
$ curl http://localhost:8082/v1/subscription-service/subscription | jq .
```
You should be able to see the Subscription that you posted. 

## Step 7 Using the reactor to track service actor state

### Overview of Step 7
This example uses a library that has implemented an efficient way to transmit metrics (APM).
Let's say when we add a `Subscription` that we want to track the number of errors and the number of successes.
If you go back to the create method (`SubscriptionServiceImpl.create`), you will notice that we do track 
the number of times `create` has been called (by calling `mgmt.increment(MGMT_CREATE_KEY);`). 

### Background of Step 7
What you might not have know is that the call to `mgmt.increment` goes to the 
[Metrik](https://github.com/advantageous/metrik) implementation provided by [QBit](https://github.com/advantageous/qbit) 
(which can be queried at runtime for back pressure controls) which sends the messages to a 
[StatsD daemon](https://github.com/etsy/statsd) which then stores them into 
[InfluxDB Time series database](https://influxdata.com/) where you can visualize them with [Grafana](http://grafana.org/)
which is a metric and analytic dashboards. Once the data is in InfluxDB there are 
[APM](https://en.wikipedia.org/wiki/Application_performance_management)
tools which can send notifications or take other actions (based on levels or anomaly detection.) 

### Details of the reactor
The library that gathers the stats efficiently is stateful and depends on active object (or rather typed Actors or 
as I call them Service Actors). This means that the stat collection wants to happen in the same thread as the Service
Actor. 

The Reactor is a class that enables 

* callbacks that execute in caller's thread (thread safe, async callbacks)
* tasks that run in the caller's thread
* repeating tasks that run in a caller's thread
* one shot after time period tasks that run in the caller's thread

The *reakt* `Reactor` is a lot like the `QBit Reactor` or the `Vert.x context`.
It allows you to enable tasks that run in actors, service actors or verticles thread.

The *reakt* `Reactor` creates ***replay promises***. Replay promises execute
in the same thread as the caller. They are "replayed" in the callers thread.

[QBit](http://advantageous.github.io/qbit/) implements a service actor model (similar to Akka type actors),
 and Vert.x implements a Reactor model (like Node.js).

QBit, for example, ensures that all method calls are queued and handled by the 
service/actor thread. You can also use the *Reakt* `Reactor` to ensure that *callbacks/promises handlers*
happen on the same thread as the caller. This allows the callbacks to be thread safe.
In this example we are forcing the callback to be replayed in the same thread as the addMethod call (in a non-blocking fashion).

The Reakt `Reactor` is a drop in replacement for QBit Reactor except that the Reakt
Reactor uses `Reakt` and QBit is moving towards `Reakt`. 
`Promise`s, async `Result`s and `Callback`s. QBit 2 and 

You can use the *Reakt* `Reactor` is not tied to QBit and you can use it with RxJava, Vert.x, or Spring Reactor 
and other similar minded projects to manage repeating tasks, tasks, and callbacks on the same thread as the caller (which you 
do not always need to do).

The `Reactor` is just an interface so you could replace it with an optimized version.


### Reactor Methods of note

Here is a high level list of Reactor methods.
* `addRepeatingTask(interval, runnable)` add a task that repeats every interval
* `runTaskAfter(afterInterval, runnable)` run a task after an interval expires
* `deferRun(Runnable runnable)` run a task on this thread as soon as you can
*  `static reactor(...)` create a reactor
*  `all(...)` create a promise that does not async return until all promises async return. (you can pass a timeout)
*  `any(...)` create a promise that does not async return until one of the promises async return. (you can pass a timeout)
* `process` process all tasks, callbacks.



A `Reactor` provides *replay promise*, which are promises whose handlers (callbacks) can be replayed on the callers thread. 
To replay the handlers on this service actors thread (`SubscriptionServiceImpl`), we can use the `Promise.invokeWithReactor` method
as follows:

#### ACTION Edit src/main/java/io/advantageous/reakt/examples/service/SubscriptionServiceImpl.java
```java

... 

@RequestMapping("/subscription-service")
public class SubscriptionServiceImpl implements SubscriptionService {
    private static final String PATH = "/subscription";
    
    ...

    @Override
    @POST(value = PATH)
    public Promise<Boolean> create(final Subscription subscription) {
        return invokablePromise(promise -> {
            mgmt.increment(MGMT_CREATE_KEY);
            ...
                    .invokeWithReactor(mgmt.reactor()); //USE THE Reactor

        });
    }

```

Track if the call to the repo was successful. 



Notice that mgmt.increment is not a thread safe calls. It keeps a local cache of counts, timings and such.
We call it from the same thread as the service actor by using the reactor (`.invokeWithReactor(mgmt.reactor())`).

#### Action finish all of the TODO items in the SubscriptionServiceImpl

Just open up the file. Reason on the TODOs and questions. There are some gems in there. 


### ACTION Run it
```
$ gradle clean build run
```

### ACTION Hit it with rest a few times
```
 $ curl -X POST http://localhost:8082/v1/subscription-service/subscription \
  -d '{"name":"test", "thirdPartyId":"1234"}' -H "Content-type: application/json" | jq .

```

Now go to [grafana](http://localhost:3003/dashboard/db/main?panelId=1&fullscreen&edit&from=now-5m&to=now) and look
 at the metrics. (Note this is a local link so we are assuming you are running the examples).
 
 
## Example using Kafka with Reakt (streams)
 
We have this message service interface.

```java
public interface MessageService {

    Promise<Boolean> publish(Message message);

}

```

We want to provide an implementation that uses Kafka to send publish a message to a Kafka topic and then
 consume that message and log the message. 
 
The `MessageService` impl will use a `Producer` that uses an invokakable promise to call call the 
Kafka producer API async, then uses the `promise.resolve` and `promise.reject` of the invokeable to 
bridge from the async Kafka world to the Reakt world. 


#### Using Kafka async lib with Reakt to publish a message
```java
    public Promise<Boolean> send(String key, String message) {
        return invokablePromise(promise ->
                producer.send(new ProducerRecord<>(topic, key, message), (m, e) -> {
                    if (m != null) {

                        logger.info("message " + message + " sent to partition(" + m.partition() + "), " +
                                "offset(" + m.offset() + ") at " + System.currentTimeMillis());

                        promise.resolve(true);
                    } else {
                        promise.reject(e);
                    }
                }));

    }

```

Now we use a `Reakt` stream to subscribe to a Kafka message stream for a give topic.
Again we use the `Reakt` labmda friendly `Stream` and adapt it to the `Kafka` stream.

#### Using Kafka async lib with Reakt to consume a stream of messages

```java
    public Promise<Boolean> consume(String topic, Stream<String> stream) {
        return invokablePromise(promise -> {
            KafkaStream<byte[], byte[]> kafkaStream =
                    consumer.createMessageStreams(
                            new HashMap<String, Integer>(){
                                {
                                    put(topic, 1);
                                }
                            }
                    ).get(topic).get(0);
            executor.submit(() -> kafkaStream.forEach(
                    data -> stream.reply(new String(data.message()))));
        });
    }
    
```    

#### Action: Modify MessageServiceImpl (src/main/java/io/advantageous/reakt/examples/service/MessageServiceImpl.java)

Just modify the file and follow the TODO instructions. 


#### Action: Modify Consumer (src/main/java/io/advantageous/reakt/examples/messaging/Consumer.java)

Just modify the file and follow the TODO instructions. 


#### Action: Modify Producer (src/main/java/io/advantageous/reakt/examples/messaging/Producer.java)

Just modify the file and follow the TODO instructions. 



### ACTION Run it
```
$ gradle clean build run
```

### ACTION Hit it with rest a few times
```
 $ curl -X POST http://localhost:8082/v1/subscription-service/message \
  -d '{"name":"test", "id":1234}' -H "Content-type: application/json" | jq .

```

