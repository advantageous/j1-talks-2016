package io.advantageous.reakt.examples.service;

import io.advantageous.reakt.examples.model.Subscription;
import io.advantageous.reakt.promise.Promise;

import java.util.UUID;

import static io.advantageous.reakt.promise.Promises.invokablePromise;

/**
 * Created by jasondaniel on 8/22/16.
 */
public class ThirdPartySubscriptionService {

    public Promise<String> create(Subscription subscription) {
        //Call Stripe to create a subscription
        return invokablePromise(promise ->
                promise.resolve(UUID.randomUUID().toString())
        );


    }

    public Promise<Boolean> update(Subscription subscription) {
        //Call Stripe to update a subscription
        return invokablePromise(promise ->
                promise.resolve(true)
        );
    }

    public Promise<Boolean> remove(String id) {
        //Call Stripe to remove a subscription
        return invokablePromise(promise ->
                promise.resolve(true)
        );
    }
}
