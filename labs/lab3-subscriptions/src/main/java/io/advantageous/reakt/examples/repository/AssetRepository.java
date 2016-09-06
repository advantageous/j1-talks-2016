package io.advantageous.reakt.examples.repository;

import com.datastax.driver.core.querybuilder.*;
import io.advantageous.reakt.examples.model.Asset;
import io.advantageous.reakt.examples.template.CassandraTemplate;
import io.advantageous.reakt.examples.template.RowMapper;
import io.advantageous.reakt.promise.Promise;

import java.net.URI;
import java.util.List;

import static io.advantageous.reakt.promise.Promises.invokablePromise;

/**
 * Created by jasondaniel on 9/6/16.
 */
public class AssetRepository {

    private final CassandraTemplate<Asset> cassandraTemplate;

    private static final String KEYSPACE = "assetKeyspace";


    private static final String TABLE_DEFINITION = "\nCREATE KEYSPACE IF NOT EXISTS  "+KEYSPACE+" with REPLICATION = " +
            " { 'class' : 'SimpleStrategy', 'replication_factor' : %d };\n" +
            "USE "+KEYSPACE+";" +
            "CREATE TABLE IF NOT EXISTS Asset (\n" +
            "                        id text,\n" +
            "                        name text,\n" +
            "                        createTime timestamp,\n" +
            "                        primary key (id, createTime)\n" +
            "                    )\n" +
            "                    WITH CLUSTERING ORDER BY ( createTime asc );";

    public AssetRepository(final int replicationFactor, final List<URI> cassandraUris) {
        cassandraTemplate = new CassandraTemplate<>(replicationFactor,
                cassandraUris, TABLE_DEFINITION, KEYSPACE);
    }

    public Promise<Boolean> store(Asset asset){
        return invokablePromise(promise -> {
                    final Insert insert = QueryBuilder.insertInto("Asset")
                            .value("id", asset.getId())
                            .value("name", asset.getName())
                            .value("createTime", System.currentTimeMillis());

                    cassandraTemplate.ifConnected("Adding asset", promise,
                            () -> cassandraTemplate.insert(promise, insert));
                }
        );
    }

    public Promise<Asset> find(String id){
        return invokablePromise(promise -> {

            final Select.Where find = QueryBuilder.select()
                    .all()
                    .from("Asset")
                    .where(QueryBuilder.eq("id", id));
            find.limit(1);

            cassandraTemplate.ifConnected("Find asset with id "+id, promise,
                    () -> cassandraTemplate.find(promise, find, map()));
        });
    }

    public Promise<Boolean> update(Asset asset) {
        return invokablePromise(promise -> {
                    if(asset.getName() == null){
                        promise.reject("Name cannot be null");
                    }
                    final Update.Where update = QueryBuilder.update("Asset")
                            .with(QueryBuilder.set("name", asset.getName()))
                            .where(QueryBuilder.eq("id", asset.getId()))
                            .and(QueryBuilder.eq("createTime", asset.getCreateTime()));

                    cassandraTemplate.ifConnected("Updating asset with id "+asset.getId(), promise,
                            () -> cassandraTemplate.update(promise, update));
                }
        );
    }

    public Promise<Boolean> remove(String id){
        return invokablePromise(promise -> {

            final Delete.Where delete = QueryBuilder.delete()
                    .from("Asset")
                    .where(QueryBuilder.eq("id", id));

            cassandraTemplate.ifConnected("Adding asset", promise,
                    () -> cassandraTemplate.delete(promise, delete));
        });
    }

    public Promise<List<Asset>> list(){
        return invokablePromise(promise -> {
            final Select.Where list = QueryBuilder.select()
                    .all()
                    .from("Asset")
                    .where();
            list.limit(1000);

            cassandraTemplate.ifConnected("Listing assets", promise,
                    () -> cassandraTemplate.list(promise, list, map()));
        });
    }

    public Promise<Boolean> connect() {
        return cassandraTemplate.connect();
    }

    public void close() {
        cassandraTemplate.close();
    }

    private RowMapper<Asset> map() {
        return row -> {
            final String id = row.getString("id");
            final String name = row.getString("name");
            final long createTime = row.getTimestamp("createTime").getTime();
            return new Asset(id, name, createTime);
        };
    }


}
