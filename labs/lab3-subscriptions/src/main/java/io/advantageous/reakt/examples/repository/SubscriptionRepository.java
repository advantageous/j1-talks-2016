package io.advantageous.reakt.examples.repository;

import io.advantageous.reakt.examples.model.Subscription;
import io.advantageous.reakt.promise.Promise;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by jasondaniel on 8/22/16.
 */
// TODO: Persist to Cassandra
public class SubscriptionRepository {
    private final Map<String, Subscription> map;

    public SubscriptionRepository(){
        map =  new TreeMap<>();
    }

    public void store(Subscription subscription, final Promise<Boolean> promise){
        map.put(subscription.getId(), subscription);
        promise.reply(true);
    }

    public void find(String id, final Promise<Subscription> promise){
        promise.reply(map.get(id));

    }

    public void update(Subscription subscription, Promise<Boolean> promise){
        promise.reply(true);
    }

    public void remove(String id, Promise<Boolean> promise){
        promise.reply(true);
    }

    public void list(Promise<List<Subscription>> promise){
        promise.reply(new ArrayList<>(map.values()));
    }
}
