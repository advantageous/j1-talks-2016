## Overview


Reakt is reactive interfaces for Java which includes: 
 * [Promises](https://github.com/advantageous/reakt/wiki/Promise),
 * [Streams](https://github.com/advantageous/reakt/wiki/Stream), 
 * [Callbacks](https://github.com/advantageous/reakt/wiki/Callback), 
 * [Async Results](https://github.com/advantageous/reakt/wiki/Result) with [Expected](https://github.com/advantageous/reakt/wiki/Expected)
 * [Circuit Breakers](https://github.com/advantageous/reakt/wiki/Breaker)
 
The emphasis is on defining interfaces that enable lambda expressions, 
and fluent APIs for asynchronous programming for Java.

Note: This mostly just provides the interfaces not the implementations. There are some starter implementations but the 
idea is that anyone can implement this. It is all about interfaces. There are be adapters for Vertx, Guava, Cassandra, etc. 
[Elekt](http://advantageous.github.io/elekt/) uses Reakt for its reactive leadership election.
[Lokate](http://advantageous.github.io/elekt/) uses Reakt for client side service discovery 
for DNS-A, DNS-SRV, Consul and Mesos/Marathon.

You can use Reakt from gradle or maven.

#### Using from maven

Reakt is published in the [maven public repo](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22io.advantageous.reakt%22).

```xml
<dependency>
    <groupId>io.advantageous.reakt</groupId>
    <artifactId>reakt</artifactId>
    <version>2.6.0.RELEASE</version>
</dependency>
```

#### Using from gradle
```xml
compile 'io.advantageous.reakt:reakt:2.6.0.RELEASE'
```

Reakt provides a fluent API for handling async calls.


#### Fluent Promise API
```java
  Promise<Employee> promise = promise()
                .then(e -> saveEmployee(e))
                .catchError(error -> logger.error("Unable to lookup employee", error));

  employeeService.lookupEmployee(33, promise);
```

Or you can handle it in one line. 

#### Fluent Promise API example using an invokeable promise
```java


  employeeService.lookupEmployee(33, 
        promise().then(e -> saveEmployee(e))
                 .catchError(error -> logger.error("Unable to lookup ", error))
        );
```


## Building and running the example

To do a complete build and run all of the tests navigate to the project folder and use gradle.

#### build and run
```
$ pwd
~/.../j1-talks-2016/labs/lab2-todo-cassandra

$ ./gradlew clean dockerTest build
```

This will run the docker containers and then run the tests.

## To run in the IDE run you first need to run downstream docker dependencies

```
$ ./gradlew startTestDocker
# then run things in IDE
```

This example works with Cassandra, InfluxDB, Grafana, and StatsD.


Let's get started.

## Step 1 implement the add operation in TodoRepo

Add the add Todo operation in the TodoRepo.

#### ACTION Edit the file ./src/main/java/io/advantageous/j1/reakt/TodoRepo and finish addTodo method
```java
package io.advantageous.j1.reakt;
...
import io.advantageous.reakt.promise.Promise;

//Used to map Guava futures used by Cassandra driver to Reakt promises
import static io.advantageous.reakt.guava.Guava.registerCallback;

//Used to return an invokeable Promise
import static io.advantageous.reakt.promise.Promises.invokablePromise;
import static io.advantageous.reakt.promise.Promises.promise;
... 

public class TodoRepo {

    private final List<URI> cassandraUris;
    private final int replicationFactor;
    private final AtomicReference<Session> sessionRef = new AtomicReference<>();
    private final Logger logger = LoggerFactory.getLogger(TodoRepo.class);

    public Promise<Boolean> addTodo(final Todo todo) {
        //Add invokeable promise
        return invokablePromise(promise ->
                ifConnected("Adding todo", promise, () -> doAddTodo(promise, todo))
        );
    }
```

When you return a promise, client code can call your method as follows:
#### INFO Calling this REPO from a service
```java
            /** Send KPI addTodo called every time the addTodo method gets called. */
            mgmt.increment("addTodo.called");
            todoRep.addTodo(todo)
                    .then(result -> {
                        logger.error("Added todo to repo");
                        promise.resolve(result);
                    })
                    .catchError(error -> {
                        logger.error("Unable to add todo to repo", error);
                        promise.reject("Unable to add todo to repo");
                    })
                    .invoke();
```


Notice you have different handlers for handling the successful outcome versus the unsuccessful outcome.

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

#### ACTION Edit the file ./src/main/java/io/advantageous/j1/reakt/TodoRepo and finish ifConnected method
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


## Step 3 Finish the doAddTodo method

#### ACTION Edit the file ./src/main/java/io/advantageous/j1/reakt/TodoRepo and finish doAddTodo method
```java
    private void doAddTodo(final Promise<Boolean> promise, final Todo todo) {

        final Insert insert = QueryBuilder.insertInto("Todo")
                .value("id", todo.getId())
                .value("createTime", todo.getCreateTime())
                .value("name", todo.getName())
                .value("description", todo.getDescription());

        registerCallback(sessionRef.get().executeAsync(insert),
                promise(ResultSet.class)
                        .catchError(promise::reject)
                        .then(resultSet -> promise.resolve(resultSet.wasApplied()))
        );
    }
```


## Step 4 Finish the addTodo method in the service impl

#### ACTION Edit the file ./src/main/java/io/advantageous/j1/reakt/TodoServiceImpl and finish addTodo method
```java
...
@RequestMapping("/todo-service")
public class TodoServiceImpl implements TodoService {
...
    @Override
    @POST(value = "/todo")
    public Promise<Boolean> addTodo(final Todo todo) {
        logger.debug("Add Todo to list {}", todo);
        return invokablePromise(promise -> {
            /** Send KPI addTodo called every time the addTodo method gets called. */
            mgmt.increment("addTodo.called");
            todoRep.addTodo(todo)
                    .then(result -> {
                        logger.error("Added todo to repo");
                        promise.resolve(result);
                    })
                    .catchError(error -> {
                        logger.error("Unable to add todo to repo", error);
                        promise.reject("Unable to add todo to repo");
                    });
        });
    }
```

A promise has to be resolved (`promise.resolve`) or rejected (`promise.reject`). 

Once you are done editing the files, you can test them.
There is a `TodoRepoTest`.

## Step 5 Run the test

#### src/test/java/i.a.j.r.TodoRepoTest
```java

@Category(DockerTest.class)
public class TodoRepoTest {

    TodoRepo todoRepo;

    @Before
    public void before() throws Exception {
        todoRepo = new TodoRepo(1, ConfigUtils.getConfig("todo").getConfig("cassandra").getUriList("uris"));
        todoRepo.connect().invokeAsBlockingPromise().get();
    }

    @Test
    public void addTodo() throws Exception {
        final Promise<Boolean> promise = todoRepo.addTodo(new Todo("Rick", "Rick", System.currentTimeMillis()))
                .invokeAsBlockingPromise();
        assertTrue(promise.success());
        assertTrue(promise.get());
    }
    ...
```


Notice the above uses a `BlockingPromise`. 
A `BlockingPromise` is very much like a Java Future. It is blocking. 
This is useful for unit testing and for legacy integration.


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

## Step 6 Test addTodo using REST interface


#### Add a TODO
```
 $ curl -X POST http://localhost:8081/v1/todo-service/todo \
 -d '{"name":"todo", "description":"hi", "id":"abc", "createTime":1234}' -H "Content-type: application/json" | jq .
```


#### Read Todos
```
$ curl http://localhost:8081/v1/todo-service/todo/ | jq .
```

You should be able to see the Todo item that you posted. 


## Step 7 Using the reactor to track service actor state

TODO

