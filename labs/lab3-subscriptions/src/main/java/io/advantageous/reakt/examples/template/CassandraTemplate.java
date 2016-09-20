package io.advantageous.reakt.examples.template;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import io.advantageous.reakt.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.datastax.driver.core.Cluster.builder;
import static io.advantageous.reakt.guava.Guava.registerCallback;
import static io.advantageous.reakt.promise.Promises.invokablePromise;
import static io.advantageous.reakt.promise.Promises.promise;
import static java.util.Arrays.asList;

/**
 * Created by jasondaniel on 8/25/16.
 */
public class CassandraTemplate<T> {
    private final List<URI> cassandraUris;
    private final int replicationFactor;
    private String tableDefinition;
    private String keySpace;

    private final AtomicReference<Session> sessionRef = new AtomicReference<>();
    private final Logger logger = LoggerFactory.getLogger(CassandraTemplate.class);


    public CassandraTemplate(final int replicationFactor, final List<URI> cassandraUris,
                                                          String tableDefinition, String keySpace){
        this.tableDefinition = tableDefinition;
        this.keySpace = keySpace;
        this.replicationFactor = replicationFactor;
        this.cassandraUris = Collections.unmodifiableList(cassandraUris);
        logger.info("Cassandra connection URIs {}", cassandraUris);
    }

    public void insert(Promise<Boolean> promise, Insert insert){

        // TODO finish the insert
//        registerCallback(sessionRef.get().executeAsync(insert),
//                promise(ResultSet.class)
//                        .catchError(promise::reject)
//                        .then(resultSet -> promise.resolve(resultSet.wasApplied()))
//        );
    }

    public void list(Promise<List<T>> promise, Select.Where select, RowMapper<T> rowMapper){

        //TODO finish this method
//        registerCallback(sessionRef.get().executeAsync(select),
//                promise(ResultSet.class)
//                        .catchError(error -> promise.reject("Problem loading entities", error))
//                        .thenSafe(resultSet -> {
//                            final List<T> list = new ArrayList<>();
//                            resultSet.forEach(row -> list.add(rowMapper.map(row)));
//                            promise.resolve(list);
//                        }));
    }

   public void update(Promise<Boolean> promise, Update.Where update){
        registerCallback(sessionRef.get().executeAsync(update),
                promise(ResultSet.class)
                        .catchError(promise::reject)
                         .then(resultSet -> promise.resolve(resultSet.wasApplied()))
        );
    }

    public void find(Promise<T> promise, Select.Where select, RowMapper<T> rowMapper){
        registerCallback(sessionRef.get().executeAsync(select),
                promise(ResultSet.class)
                        .catchError(error -> promise.reject("Problem loading entity", error))
                        .thenSafe(resultSet -> {
                            promise.resolve(rowMapper.map(resultSet.one()));
                        }));
    }

    public void delete(Promise<Boolean> promise, Delete.Where delete){
        registerCallback(sessionRef.get().executeAsync(delete),
                promise(ResultSet.class)
                        .catchError(promise::reject)
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
        final Cluster.Builder builder = builder();
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
            final String ddl = String.format(tableDefinition, replicationFactor);

            final String[] lines = ddl.split(";");

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

            session.execute("USE "+keySpace);
            promise.resolve(session);
        }).start();


    }

    public boolean isConnected() {
        return sessionRef.get() != null && !sessionRef.get().isClosed();
    }

    public void ifConnected(final String operation,
                             final Promise<?> promise, final Runnable runnable) {
        //TODO finish this
//        // If we are not connected, try to connect, but fail this request.
//        if (!isConnected()) {
//            forceConnect();
//            //Promise rejected because we were not connected.
//            promise.reject("Not connected to cassandra for operation " + operation);
//        } else {
//            // Try running the operation
//            try {
//                runnable.run();
//            } catch (Exception ex) {
//                //Operation failed, exit
//                promise.reject("Error running " + operation, ex);
//            }
//        }

        // How does this compare with using a Breaker?
        // Could you use the instructions and ideas from the Breaker lab with this?
        // If so, what would you do?
    }

    private void forceConnect() {
        this.connectInternal()
                .catchError(error -> logger.error("Can't connect to cassandra {}", error))
                .then(sessionRef::set)
                .invoke();
    }

    public void close() {
        if (isConnected()) {
            try {
                sessionRef.get().close();
            } catch (Exception ex) {
                logger.error("Error closing session");
            }
        }
    }
}
