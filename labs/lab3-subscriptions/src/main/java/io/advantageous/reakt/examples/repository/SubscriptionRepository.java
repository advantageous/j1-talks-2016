package io.advantageous.reakt.examples.repository;

import com.datastax.driver.core.querybuilder.*;
import io.advantageous.reakt.examples.template.RowMapper;
import io.advantageous.reakt.examples.model.Subscription;
import io.advantageous.reakt.examples.template.CassandraTemplate;
import io.advantageous.reakt.promise.Promise;

import java.net.URI;
import java.util.*;

import static io.advantageous.reakt.promise.Promises.invokablePromise;
import static io.advantageous.reakt.promise.Promises.promise;
import static java.util.Arrays.asList;

/**
 * Created by jasondaniel on 8/22/16.
 */

public class SubscriptionRepository {
    private final CassandraTemplate<Subscription> cassandraTemplate;

    private static final String TABLE_DEFINITION = "\nCREATE KEYSPACE IF NOT EXISTS  subscriptionKeyspace with REPLICATION = " +
                                                   " { 'class' : 'SimpleStrategy', 'replication_factor' : %d };\n" +
                                                   "USE subscriptionKeyspace;" +
                                                   "CREATE TABLE IF NOT EXISTS Subscription (\n" +
                                                   "                        id text,\n" +
                                                   "                        name text,\n" +
                                                   "                        thirdPartyId text,\n" +
                                                   "                        createTime timestamp,\n" +
                                                   "                        primary key (id, createTime)\n" +
                                                   "                    )\n" +
                                                   "                    WITH CLUSTERING ORDER BY ( createTime asc );";

    private static final String KEYSPACE = "subscriptionKeyspace";


    public SubscriptionRepository(final int replicationFactor, final List<URI> cassandraUris) {
        cassandraTemplate = new CassandraTemplate<>(replicationFactor,
                                                    cassandraUris, TABLE_DEFINITION, KEYSPACE);
    }

    public Promise<Boolean> store(Subscription subscription){
        return invokablePromise(promise -> {
                    final Insert insert = QueryBuilder.insertInto("Subscription")
                            .value("id", subscription.getId())
                            .value("name", subscription.getName())
                            .value("thirdPartyId", subscription.getThirdPartyId())
                            .value("createTime", System.currentTimeMillis());

                    cassandraTemplate.ifConnected("Adding subscription", promise,
                            () -> cassandraTemplate.insert(promise, insert));
                }
        );
    }

    public Promise<Subscription> find(String id){
        return invokablePromise(promise -> {

            final Select.Where find = QueryBuilder.select()
                    .all()
                    .from("Subscription")
                    .where(QueryBuilder.eq("id", id));
            find.limit(1);

            cassandraTemplate.ifConnected("Find subscription with id "+id, promise,
                    () -> cassandraTemplate.find(promise, find, map()));
        });
    }

    public Promise<Boolean> update(Subscription subscription) {
        return invokablePromise(promise -> {
            if(subscription.getName() == null){
                promise.reject("Name cannot be null");
            }
            final Update.Where update = QueryBuilder.update("Subscription")
                    .with(QueryBuilder.set("name", subscription.getName()))
                    .where(QueryBuilder.eq("id", subscription.getId()))
                    .and(QueryBuilder.eq("createTime", subscription.getCreateTime()));

            cassandraTemplate.ifConnected("Updating subscription with id "+subscription.getId(), promise,
                            () -> cassandraTemplate.update(promise, update));
        }
        );
    }

    public Promise<Boolean> remove(String id){
        return invokablePromise(promise -> {

            final Delete.Where delete = QueryBuilder.delete()
                    .from("Subscription")
                    .where(QueryBuilder.eq("id", id));

            cassandraTemplate.ifConnected("Adding subscription", promise,
                    () -> cassandraTemplate.delete(promise, delete));
        });
    }

    public Promise<List<Subscription>> list(){
        return invokablePromise(promise -> {
            final Select.Where list = QueryBuilder.select()
                    .all()
                    .from("Subscription")
                    .where();
            list.limit(1000);

            cassandraTemplate.ifConnected("Listing subscriptions", promise,
                            () -> cassandraTemplate.list(promise, list, map()));
        });
    }

    public Promise<Boolean> connect() {
        return cassandraTemplate.connect();
    }

    public void close() {
        cassandraTemplate.close();
    }

    private RowMapper<Subscription> map() {
        return row -> {
            final String id = row.getString("id");
            final String name = row.getString("name");
            final String thirdPartyId = row.getString("thirdPartyId");
            final long createTime = row.getTimestamp("createTime").getTime();
            return new Subscription(id, name, thirdPartyId, createTime);
        };
    }
}
