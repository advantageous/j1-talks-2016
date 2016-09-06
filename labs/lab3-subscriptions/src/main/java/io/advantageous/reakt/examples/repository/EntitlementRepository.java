package io.advantageous.reakt.examples.repository;

import com.datastax.driver.core.querybuilder.*;
import io.advantageous.reakt.examples.model.Entitlement;
import io.advantageous.reakt.examples.template.CassandraTemplate;
import io.advantageous.reakt.examples.template.RowMapper;
import io.advantageous.reakt.promise.Promise;

import java.net.URI;
import java.util.List;

import static io.advantageous.reakt.promise.Promises.invokablePromise;

/**
 * Created by jasondaniel on 9/6/16.
 */
public class EntitlementRepository {
    private final CassandraTemplate<Entitlement> cassandraTemplate;

    private static final String KEYSPACE = "entitlementKeyspace";


    private static final String TABLE_DEFINITION = "\nCREATE KEYSPACE IF NOT EXISTS  "+KEYSPACE+" with REPLICATION = " +
            " { 'class' : 'SimpleStrategy', 'replication_factor' : %d };\n" +
            "USE "+KEYSPACE+";" +
            "CREATE TABLE IF NOT EXISTS Entitlement (\n" +
            "                        asset_id text,\n" +
            "                        subscription_id text,\n" +
            "                        createTime timestamp,\n" +
            "                        primary key ((asset_id, subscription_id), createTime)\n" +
            "                    )\n" +
            "                    WITH CLUSTERING ORDER BY ( createTime asc );";

    public EntitlementRepository(final int replicationFactor, final List<URI> cassandraUris) {
        cassandraTemplate = new CassandraTemplate<>(replicationFactor,
                cassandraUris, TABLE_DEFINITION, KEYSPACE);
    }

    public Promise<Boolean> store(Entitlement entitlement){
        return invokablePromise(promise -> {
                    final Insert insert = QueryBuilder.insertInto("Entitlement")
                            .value("asset_id", entitlement.getAssetId())
                            .value("subscription_id", entitlement.getSubscriptionId())
                            .value("createTime", System.currentTimeMillis());

                    cassandraTemplate.ifConnected("Adding entitlement", promise,
                            () -> cassandraTemplate.insert(promise, insert));
                }
        );
    }

    public Promise<Entitlement> find(String assetId, String subscriptionId){
        return invokablePromise(promise -> {

            final Select.Where find = QueryBuilder.select()
                    .all()
                    .from("Entitlement")
                    .where(QueryBuilder.eq("asset_id", assetId))
                    .and(QueryBuilder.eq("subscription_id", subscriptionId));
            find.limit(1);

            cassandraTemplate.ifConnected("Find entitlement with composite id "+assetId+","+subscriptionId, promise,
                    () -> cassandraTemplate.find(promise, find, map()));
        });
    }


    public Promise<Boolean> remove(String assetId, String subscriptionId){
        return invokablePromise(promise -> {

            final Delete.Where delete = QueryBuilder.delete()
                    .from("Entitlement")
                    .where(QueryBuilder.eq("asset_id", assetId))
                    .and(QueryBuilder.eq("subscription_id", subscriptionId));

            cassandraTemplate.ifConnected("Removing entitlement", promise,
                    () -> cassandraTemplate.delete(promise, delete));
        });
    }

    public Promise<List<Entitlement>> list(){
        return invokablePromise(promise -> {
            final Select.Where list = QueryBuilder.select()
                    .all()
                    .from("Entitlement")
                    .where();
            list.limit(1000);

            cassandraTemplate.ifConnected("Listing entitlements", promise,
                    () -> cassandraTemplate.list(promise, list, map()));
        });
    }

    public Promise<Boolean> connect() {
        return cassandraTemplate.connect();
    }

    public void close() {
        cassandraTemplate.close();
    }

    private RowMapper<Entitlement> map() {
        return row -> {
            final String assetId = row.getString("asset_id");
            final String subscriptionId = row.getString("subscription_id");
            final long createTime = row.getTimestamp("createTime").getTime();
            return new Entitlement(assetId, subscriptionId, createTime);
        };
    }

}
