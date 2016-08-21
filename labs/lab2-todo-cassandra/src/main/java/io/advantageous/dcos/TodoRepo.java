package io.advantageous.dcos;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import io.advantageous.reakt.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.datastax.driver.core.Cluster.Builder;
import static com.datastax.driver.core.Cluster.builder;
import static io.advantageous.reakt.guava.Guava.registerCallback;
import static io.advantageous.reakt.promise.Promises.invokablePromise;
import static io.advantageous.reakt.promise.Promises.promise;
import static java.util.Arrays.asList;

public class TodoRepo {

    private final List<URI> cassandraUris;
    private final int replicationFactor;
    private final AtomicReference<Session> sessionRef = new AtomicReference<>();
    private final Logger logger = LoggerFactory.getLogger(TodoRepo.class);

    TodoRepo(final int replicationFactor, final List<URI> cassandraUris) {
        this.replicationFactor = replicationFactor;
        this.cassandraUris = Collections.unmodifiableList(cassandraUris);
        logger.info("Cassandra connection URIs {}", cassandraUris);
    }

    TodoRepo(final int replicationFactor, final URI... connections) {
        this(replicationFactor, asList(connections));
    }

    public Promise<Boolean> addTodo(final Todo todo) {
        return invokablePromise(promise ->
                ifConnected("Adding todo", promise, () -> doAddTodo(promise, todo))
        );
    }

    public Promise<List<Todo>> loadTodos() {
        return invokablePromise(promise ->
                ifConnected("Load todos", promise, () -> doLoadTodos(promise))
        );
    }

    private void doLoadTodos(Promise<List<Todo>> promise) {
        final Select.Where query = QueryBuilder.select()
                .all()
                .from("Todo")
                .where();
        query.limit(1000);

        registerCallback(sessionRef.get().executeAsync(query),
                promise(ResultSet.class)
                        .catchError(error -> promise.reject("Problem loading Todos", error))
                        .thenSafe(resultSet -> {
                            final List<Todo> todoList = new ArrayList<>();
                            resultSet.forEach(row -> todoList.add(mapTodoFromRow(row)));
                            promise.resolve(todoList);
                        }));

    }

    private Todo mapTodoFromRow(final Row row) {
        final String name = row.getString("name");
        final String description = row.getString("description");
        final long createTime = row.getTimestamp("createTime").getTime();
        return new Todo(name, description, createTime);
    }

    private void doAddTodo(final Promise<Boolean> promise, final Todo todo) {

        final Insert insert = QueryBuilder.insertInto("Todo")
                .value("id", todo.getId())
                .value("createTime", todo.getCreateTime())
                .value("name", todo.getName())
                .value("description", todo.getDescription());

        registerCallback(sessionRef.get().executeAsync(insert),
                promise(ResultSet.class)
                        .catchError(error -> promise.reject(error))
                        .then(resultSet -> promise.resolve(resultSet.wasApplied()))
        );
    }


    public Promise<Boolean> connect() {
        return invokablePromise(promise ->
                connectInternal()
                        .catchError(promise::reject)
                        .then(session -> {
                            sessionRef.set(session);
                            promise.resolve(true);
                        }).invoke()
        );
    }

    private Promise<Session> connectInternal() {
        return invokablePromise(promise -> {
            try {
                doConnect(promise);
            } catch (Exception ex) {
                promise.reject("Unable to use Cassandra API", ex);
            }
        });
    }

    private void doConnect(Promise<Session> promise) {
        final Builder builder = builder();
        cassandraUris.forEach(cassandraURI -> builder.withPort(cassandraURI.getPort())
                .addContactPoints(cassandraURI.getHost()).build());

        final Promise<Session> sessionPromise = promise(Session.class)
                .catchError(error -> promise.reject("Unable to load initial session", error))
                .then(session -> buildDBIfNeeded(session, promise));

        registerCallback(
                builder.build().connectAsync(),
                sessionPromise);
    }

    private synchronized void buildDBIfNeeded(final Session session,
                                              final Promise<Session> promise) {

        if (isConnected()) {
            return;
        }

        new Thread(() -> {
            final String todoTableAscending = String.format("\nCREATE KEYSPACE IF NOT EXISTS  todoKeyspace with REPLICATION = " +
                    " { 'class' : 'SimpleStrategy', 'replication_factor' : %d };\n" +
                    "USE todoKeyspace;" +
                    "CREATE TABLE IF NOT EXISTS Todo (\n" +
                    "                        id text,\n" +
                    "                        name text,\n" +
                    "                        description text,\n" +
                    "                        createTime timestamp,\n" +
                    "                        primary key (id, createTime)\n" +
                    "                    )\n" +
                    "                    WITH CLUSTERING ORDER BY ( createTime asc );", replicationFactor);

            final String[] lines = todoTableAscending.split(";");

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
                    if (!session.execute(line).wasApplied()) {
                        logger.info("unable to process last line {}", line);
                    }
                } catch (Exception ex) {
                    logger.error("unable to run command " + line, ex);
                }
            }

            session.execute("USE todoKeyspace");
            promise.resolve(session);
        }).start();


    }

    public boolean isConnected() {
        return sessionRef.get() != null && !sessionRef.get().isClosed();
    }

    public void ifConnected(final String operation,
                            final Promise<?> promise, final Runnable runnable) {
        if (!isConnected()) {
            forceConnect();
            promise.reject("Not connected to cassandra for operation " + operation);
        } else {
            try {
                runnable.run();
            } catch (Exception ex) {
                promise.reject("Error running " + operation, ex);
            }
        }
    }


    private void forceConnect() {
        this.connectInternal()
                .catchError(error -> logger.error("Can't connect to cassandra {}", error))
                .then(sessionRef::set)
                .invoke();
    }
}
