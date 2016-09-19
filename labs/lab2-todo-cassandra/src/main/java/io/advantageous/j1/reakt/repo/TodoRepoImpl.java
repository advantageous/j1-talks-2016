package io.advantageous.j1.reakt.repo;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import io.advantageous.discovery.DiscoveryService;
import io.advantageous.j1.reakt.MessageQueue;
import io.advantageous.j1.reakt.Todo;
import io.advantageous.qbit.admin.ServiceManagementBundle;
import io.advantageous.qbit.annotation.QueueCallback;
import io.advantageous.qbit.annotation.QueueCallbackType;
import io.advantageous.reakt.Breaker;
import io.advantageous.reakt.Expected;
import io.advantageous.reakt.cassandra.AsyncSession;
import io.advantageous.reakt.promise.Promise;
import io.advantageous.reakt.promise.Promises;
import io.advantageous.reakt.reactor.Reactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.datastax.driver.core.Cluster.Builder;
import static com.datastax.driver.core.Cluster.builder;
import static com.datastax.driver.core.querybuilder.QueryBuilder.*;
import static io.advantageous.reakt.guava.Guava.futureToPromise;
import static io.advantageous.reakt.promise.Promises.invokablePromise;


public class TodoRepoImpl implements TodoRepo {

    private final int replicationFactor;
    private final Logger logger = LoggerFactory.getLogger(TodoRepoImpl.class);
    private final Reactor reactor;
    private final ServiceManagementBundle serviceMgmt;
    private final DiscoveryService discoveryService;
    private final URI cassandraURI;
    private final AtomicLong cassandraErrors = new AtomicLong();
    private Breaker<AsyncSession> sessionBreaker = Breaker.opened();
    private int notConnectedCount;
    private MessageQueue messageQueue = new MessageQueue();

    public TodoRepoImpl(final int replicationFactor,
                        final URI cassandraURI,
                        final ServiceManagementBundle mgmt,
                        final DiscoveryService discoveryService) {
        this.replicationFactor = replicationFactor;
        reactor = mgmt.reactor();
        serviceMgmt = mgmt;
        this.discoveryService = discoveryService;
        this.cassandraURI = cassandraURI;
    }

    @QueueCallback(QueueCallbackType.INIT)
    private void start() {

        reactor.runTaskAfter(Duration.ofSeconds(60), () -> {
            logger.info("Registering health check and recovery for repo");
            reactor.addRepeatingTask(Duration.ofSeconds(30), this::circuitBreakerTest);
        });

    }

    private void circuitBreakerTest() {
        sessionBreaker.ifBroken(() -> {
            serviceMgmt.increment("repo.breaker.broken");
            //Clean up the old session.
            sessionBreaker.cleanup(session -> {
                try {
                    if (!session.getSession().isClosed()) {
                        session.close();
                    }
                } catch (Exception ex) {
                    logger.warn("unable to clean up old session", ex);
                }
            });
            //Connect to repo.
            connect().catchError(error -> {
                notConnectedCount++;
                logger.error("Not connected to repo " + notConnectedCount, error);
                serviceMgmt.recordLevel("repo.not.connected", notConnectedCount);
                serviceMgmt.increment("repo.connect.error");
                serviceMgmt.increment("repo.connect.error." + error.getClass().getSimpleName().toLowerCase());
                if (notConnectedCount > 10) {
                    logger.error("Attempts to reconnect to Cassandra have failed. Marking repo as failed.");
                    serviceMgmt.increment("repo.connect.error.fatal");
                    serviceMgmt.setFailingWithError(error);
                }
            }).thenSafe(connected -> {
                if (serviceMgmt.isFailing()) {
                    serviceMgmt.increment("repo.connect.recover");
                    serviceMgmt.recover();
                }
                notConnectedCount = 0;
            }).invokeWithReactor(reactor);
        });
    }


    /**
     * This always has to load a Todo with a created time.
     * The Todo item might not exist so use Reakt Expected (which is like Java Optional).
     * You are expecting this to return a Todo, but it might not.
     */
    @Override
    public Promise<Expected<Todo>> loadTodo(final String id) {
        logger.info("Load Todo called");
        return invokablePromise(returnPromise -> sessionBreaker
                .ifBroken(() -> {
                    final String message = "Not connected to cassandra while loading a todo item";
                    returnPromise.reject(message);
                    logger.error(message);
                    serviceMgmt.increment("cassandra.breaker.broken");
                })
                .ifOperational(session ->

                        session.execute(select().all().from("Todo")
                                .where(eq("id", id))
                                .limit(1))
                                .catchError(error -> {
                                    //Failure.
                                    recordCassandraError("load.todo", error);
                                    returnPromise.reject("Problem loading Todos", error);
                                }).thenSafe(resultSet -> {
                                    final Row row = resultSet.one();
                                    //Nothing found so send them an empty result.
                                    if (row == null) {
                                        returnPromise.resolve(Expected.empty());
                                    } else {
                                        final Todo todo = mapTodoFromRow(row);
                                        // The Todo has a created time, so send it back now.
                                        if (todo.getCreatedTime() != 0) {
                                            returnPromise.resolve(Expected.of(todo));
                                        } else {
                                            //We need to find the create time before we send it back.
                                            //Next time it is updated, it will have the created time.
                                            loadFirstTodoCreateTime(session, id).thenSafe(createdTime -> {
                                                returnPromise.resolve(Expected.of(new Todo(todo, createdTime)));
                                            }).catchError(error -> returnPromise.reject("Created time not found"))
                                                    .invoke();
                                        }
                                    }
                                }
                        ).invokeWithReactor(reactor)

                )
        );
    }

    private Promise<Long> loadFirstTodoCreateTime(final AsyncSession session,
                                                  final String id) {
        return invokablePromise(returnPromise ->

                session.execute(select().all().from("TodoLookup")
                        .where(eq("id", id)).and(gte("updatedTime", 0L)).limit(1))

                        .catchError(error -> recordCassandraError("load.todo", error))
                        .thenSafe(resultSet -> {
                                    final Row row = resultSet.one();
                                    if (row == null) {
                                        returnPromise.resolve(-1L);
                                    } else {
                                        returnPromise.resolve((row.getTimestamp("updatedTime")
                                                .getTime()));
                                    }
                                }
                        ).invokeWithReactor(reactor));

    }

    @Override
    public Promise<Boolean> addTodo(final Todo todo) {
        logger.info("Add Todo called");
        return invokablePromise(promise -> sessionBreaker
                .ifBroken(() -> {
                    final String message = "Not connected to cassandra while adding todo";
                    promise.reject(message);
                    logger.error(message);
                    serviceMgmt.increment("cassandra.breaker.broken");
                })
                .ifOperational(session ->
                        doAddTodo(todo, promise, session)
                )
        );
    }

    // version w/o any example
//    private void doAddTodo(final Todo todo,
//                           final Promise<Boolean> returnPromise,
//                           final Session session) {
//
//        reactor.all(Duration.ofSeconds(30),
//                //Call to save Todo item in two table, don't respond until both calls come back from Cassandra.
//                // First call to cassandra.
//                futureToPromise(
//                        session.executeAsync(insertInto("Todo")
//                                .value("id", todo.getId())
//                                .value("updatedTime", todo.getUpdatedTime())
//                                .value("createdTime", todo.getCreatedTime())
//                                .value("name", todo.getName())
//                                .value("description", todo.getDescription()))
//                ).catchError(error -> recordCassandraError("add.todo", error))
//                        .thenSafe(resultSet -> handleResultFromAdd(resultSet, "add.todo")),
//                // Second call to cassandra.
//                futureToPromise(
//                        session.executeAsync(insertInto("TodoLookup")
//                                .value("id", todo.getId())
//                                .value("updatedTime", todo.getUpdatedTime()))
//                ).catchError(error -> recordCassandraError("add.lookup", error))
//                        .thenSafe(resultSet -> handleResultFromAdd(resultSet, "add.lookup"))
//        ).catchError(returnPromise::reject)
//                .then(v -> returnPromise.resolve(true))
//                .invoke();
//
//    }


    private void doAddTodo(final Todo todo,
                           final Promise<Boolean> returnPromise,
                           final AsyncSession session) {

        //Send to the queue and two tables in Cassandra at the same time,
        // wait until one of the succeed and then resolve the original call.
        reactor.any(Duration.ofSeconds(30),
                messageQueue.sendToQueue(todo)
                        .catchError(error -> logger.error("Send to queue failed", error))
                        .thenSafe(enqueued -> logger.info("Sent to queue")),
                reactor.all(Duration.ofSeconds(15),

                        //Call to save Todo item in two table, don't respond until both calls come back from Cassandra.
                        // First call to NoSQL DB.
                        session.execute(insertInto("Todo")
                                .value("id", todo.getId())
                                .value("updatedTime", todo.getUpdatedTime())
                                .value("createdTime", todo.getCreatedTime())
                                .value("name", todo.getName())
                                .value("description", todo.getDescription()))
                                .catchError(error -> recordCassandraError("add.todo", error))
                                .thenSafe(resultSet -> handleResultFromAdd(resultSet, "add.todo")),
                        // Second call to NoSQL DB.
                        session.execute(insertInto("TodoLookup")
                                .value("id", todo.getId())
                                .value("updatedTime", todo.getUpdatedTime()))
                                .catchError(error -> recordCassandraError("add.lookup", error))
                                .thenSafe(resultSet -> handleResultFromAdd(resultSet, "add.lookup"))
                )
        ).catchError(returnPromise::reject)
                .then(v -> returnPromise.resolve(true)).invoke();

    }

    private void handleResultFromAdd(final ResultSet resultSet, final String operation) {

        if (resultSet.wasApplied()) {
            serviceMgmt.increment(operation + ".success");
        } else {
            serviceMgmt.increment(operation + ".fail.not.added");
            throw new IllegalStateException("Unable to save data to todo table but no exceptions " +
                    "reported from cassandra");
        }
    }

    private void recordCassandraError(final String what, final Throwable error) {
        serviceMgmt.increment(what + ".fail");
        serviceMgmt.increment(what + ".fail" +
                error.getClass().getName().toLowerCase());
        cassandraErrors.incrementAndGet();
        serviceMgmt.increment("cassandra.error");
    }

    @Override
    public Promise<List<Todo>> loadTodos() {
        return invokablePromise(returnPromise -> sessionBreaker
                .ifBroken(() -> {
                    final String message = "Not connected to cassandra while adding todo";
                    returnPromise.reject(message);
                    logger.error(message);
                })//ifBroken
                .ifOperational(session ->

                        session.execute(select().all().from("Todo").where().limit(1000))
                                .catchError(error -> {
                                    recordCassandraError("load.todo", error);
                                    returnPromise.reject("Problem loading Todos", error);
                                }).thenSafe(resultSet ->
                                returnPromise.resolve(
                                        resultSet.all().stream().map(this::mapTodoFromRow)
                                                .collect(Collectors.toList())
                                )
                        ).invokeWithReactor(reactor)
                )//ifOperational
        );
    }


    private Todo mapTodoFromRow(final Row row) {
        final String name = row.getString("name");
        final String description = row.getString("description");
        final Date createdDate = row.getTimestamp("createdTime");
        final long createTime = createdDate == null ? 0L : createdDate.getTime();
        final String id = row.getString("id");
        return new Todo(name, description, createTime, id, row.getTimestamp("updatedTime").getTime());
    }


    @Override
    public Promise<Boolean> connect() {
        return invokablePromise(promise -> {
            serviceMgmt.increment("connect.called");

            discoveryService.lookupService(cassandraURI).thenSafe(cassandraUris -> {
                serviceMgmt.increment("discovery.service.success");

                final Builder builder = builder();
                cassandraUris.forEach(cassandraURI1 -> builder.withPort(cassandraURI1.getPort())
                        .addContactPoints(cassandraURI1.getHost()).build());

                futureToPromise(builder.build().connectAsync()) //Cassandra / Guava Reakt bridge.
                        .catchError(error -> promise.reject("Unable to load initial session", error))
                        .then(sessionToInitialize ->
                                buildDBIfNeeded(sessionToInitialize)
                                        .thenSafe(session -> {
                                            cassandraErrors.set(0);
                                            sessionBreaker = Breaker.operational(new AsyncSession(session), 10, theSession ->
                                                    !theSession.getSession().isClosed() && cassandraErrors.get() > 25
                                            );
                                            promise.resolve(true);
                                        })
                                        .catchError(error ->
                                                promise.reject(
                                                        "Unable to create or initialize session", error)
                                        ).invokeWithReactor(reactor)
                        ).invokeWithReactor(reactor);

            }).catchError(error -> serviceMgmt.increment("discovery.service.fail")).invokeWithReactor(reactor);

        });
    }


    private Promise<Session> buildDBIfNeeded(final Session sessionToInitialize) {

        return Promises.invokablePromise(promise ->
                new Thread(() -> {
                    runBuildDBIfNeededCQLScript(sessionToInitialize);
                    sessionToInitialize.execute("USE todoKeyspace2");
                    promise.resolve(sessionToInitialize);
                }).start());
    }

    private void runBuildDBIfNeededCQLScript(Session sessionToInitialize) {
        final String todoTable = String.format("\nCREATE KEYSPACE IF NOT EXISTS  todoKeyspace2 with REPLICATION = " +
                " { 'class' : 'SimpleStrategy', 'replication_factor' : %d };\n" +
                "USE todoKeyspace2;" +
                "CREATE TABLE IF NOT EXISTS Todo (\n" +
                "                        id text,\n" +
                "                        name text,\n" +
                "                        version bigint,\n" +
                "                        description text,\n" +
                "                        updatedTime timestamp,\n" +
                "                        createdTime timestamp,\n" +
                "                        primary key (id, updatedTime)\n" +
                "                    )\n" +
                "                    WITH CLUSTERING ORDER BY ( updatedTime desc ); \n" +
                "CREATE TABLE IF NOT EXISTS TodoLookup (\n" +
                "                        id text,\n" +
                "                        updatedTime timestamp,\n" +
                "                        primary key (id, updatedTime)\n" +
                "                    )\n" +
                "                    WITH CLUSTERING ORDER BY ( updatedTime asc );", replicationFactor);

        System.out.println(todoTable);


        final String[] lines = todoTable.split(";");

        for (final String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            System.out.println(line);
        }

        for (final String line : lines) {

            if (line.trim().isEmpty()) {
                continue;
            }
            try {
                if (!sessionToInitialize.execute(line).wasApplied()) {
                    logger.info("unable to process last line {}", line);
                }
            } catch (Exception ex) {
                logger.error("unable to run command " + line, ex);
            }
        }
    }

    @QueueCallback(QueueCallbackType.SHUTDOWN)
    @Override
    public void close() {
        sessionBreaker.cleanup(session -> {
            session.close().then(ret -> logger.debug("Session closed"))
                    .catchError(error -> logger.error("Unable to close session", error))
                    .invoke();
        });
    }
}
