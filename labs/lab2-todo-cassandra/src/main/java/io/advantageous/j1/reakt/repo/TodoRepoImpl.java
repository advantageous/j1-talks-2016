package io.advantageous.j1.reakt.repo;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import io.advantageous.discovery.DiscoveryService;
import io.advantageous.j1.reakt.Todo;
import io.advantageous.qbit.admin.ServiceManagementBundle;
import io.advantageous.qbit.annotation.QueueCallback;
import io.advantageous.qbit.annotation.QueueCallbackType;
import io.advantageous.reakt.Breaker;
import io.advantageous.reakt.promise.Promise;
import io.advantageous.reakt.promise.Promises;
import io.advantageous.reakt.reactor.Reactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.datastax.driver.core.Cluster.Builder;
import static com.datastax.driver.core.Cluster.builder;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
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
    private Breaker<Session> sessionBreaker = Breaker.opened();
    private int notConnectedCount;

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
                    if (!session.isClosed()) {
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

    private void doAddTodo(final Todo todo,
                           final Promise<Boolean> returnPromise,
                           final Session session) {

        final Promise<ResultSet> saveToTodoTablePromise = futureToPromise(
                session.executeAsync(insertInto("Todo")
                .value("id", todo.getId())
                .value("updatedTime", todo.getUpdatedTime())
                .value("createdTime", todo.getCreatedDate())
                .value("name", todo.getName())
                .value("description", todo.getDescription()))
        ).catchError(error -> {
            serviceMgmt.increment("add.todo.fail");
            serviceMgmt.increment("add.todo.fail." +
                    error.getClass().getName().toLowerCase());
            recordCassandraError();
        }).thenSafe(resultSet -> {
            if (resultSet.wasApplied()) {
                serviceMgmt.increment("add.todo.success");
            } else {
                serviceMgmt.increment("add.todo.fail.not.added");
                throw new IllegalStateException("Unable to save data to todod table but no exceptions " +
                        "reported from cassandra");
            }
        });

        final Promise<ResultSet> saveToLookupTable = futureToPromise(
                session.executeAsync(insertInto("TodoLookup")
                        .value("id", todo.getId())
                        .value("updatedTime", todo.getUpdatedTime()))
        ).catchError(error -> {
            serviceMgmt.increment("add.lookup.fail");
            serviceMgmt.increment("add.lookup.fail." +
                    error.getClass().getName().toLowerCase());
            recordCassandraError();
        }).thenSafe(resultSet -> {
            if (resultSet.wasApplied()) {
                serviceMgmt.increment("add.todo.success");
            } else {
                serviceMgmt.increment("add.todo.fail.not.added");
                throw new IllegalStateException("Unable to save data to lookup table but no exceptions " +
                        "reported from cassandra");
            }
        });

        reactor
                .all(Duration.ofSeconds(30),
                        saveToTodoTablePromise, saveToLookupTable)
                .catchError(returnPromise::reject)
                .then( v-> returnPromise.resolve(true))
                .invoke();

    }

    private void recordCassandraError() {
        cassandraErrors.incrementAndGet();
        serviceMgmt.increment("cassandra.error");
    }

    @Override
    public Promise<List<Todo>> loadTodos() {
        return invokablePromise(promise -> sessionBreaker
                .ifBroken(() -> {
                    final String message = "Not connected to cassandra while adding todo";
                    promise.reject(message);
                    logger.error(message);
                })//ifBroken
                .ifOperational(session ->
                        futureToPromise(
                                session.executeAsync(select().all().from("Todo").where().limit(1000))
                        ).catchError(error -> {
                            recordCassandraError();
                            promise.reject("Problem loading Todos", error);
                        }).thenSafe(resultSet ->
                                promise.resolve(
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
        return new Todo(name, description, createTime, id,  row.getTimestamp("updatedTime").getTime());
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
                                            sessionBreaker = Breaker.operational(session, 10, theSession->
                                                !theSession.isClosed() && cassandraErrors.incrementAndGet() > 25
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
        sessionBreaker.cleanup(Session::close);
    }
}
