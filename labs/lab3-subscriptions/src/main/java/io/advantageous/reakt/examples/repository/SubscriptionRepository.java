package io.advantageous.reakt.examples.repository;

import io.advantageous.reakt.examples.model.Subscription;
import io.advantageous.reakt.promise.Promise;

import java.util.*;

import static io.advantageous.reakt.promise.Promises.invokablePromise;

/**
 * Created by jasondaniel on 8/22/16.
 */
// TODO: Persist to Cassandra
public class SubscriptionRepository {
    private final Map<String, Subscription> map;

    public SubscriptionRepository(){
        map =  new TreeMap<>();
    }

    public Promise<Subscription> store(Subscription subscription){
        return invokablePromise(promise -> {
            subscription.setId(UUID.randomUUID().toString());
            map.put(subscription.getId(), subscription);
            promise.resolve(subscription);
        });
    }

    public Promise<Subscription> find(String id){
        return invokablePromise(promise ->
            promise.resolve(map.get(id))
        );
    }

    public Promise<Boolean> update(Subscription subscription) {
        return invokablePromise(promise -> {
            Subscription current = map.get(subscription.getId());

            if (current == null) {
                promise.reject(" Subscription does not exist");
            }
            else {
                if (subscription.getName() != null) {
                    current.setName(subscription.getName());
                }

                promise.resolve(true);
            }
        });
    }

    public Promise<Boolean> remove(String id){
        return invokablePromise(promise -> {
            map.remove(id);
            promise.resolve(true);
        });
    }

    public Promise<List<Subscription>> list(){
        return invokablePromise(promise ->
            promise.resolve(new ArrayList<>(map.values()))
        );
    }
}
