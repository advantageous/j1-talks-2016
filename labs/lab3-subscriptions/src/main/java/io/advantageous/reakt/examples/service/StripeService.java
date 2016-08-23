package io.advantageous.reakt.examples.service;

import io.advantageous.reakt.examples.model.Subscription;
import io.advantageous.reakt.promise.Promise;

import java.util.UUID;

/**
 * Created by jasondaniel on 8/22/16.
 */
public class StripeService {

    public static void create(final Promise<String> promise){
        //Call Stripe to create a subscription
        promise.reply(UUID.randomUUID().toString());
    }

    public static void update(Subscription subscription, Promise<Boolean> promise){
        //Call Stripe to update a subscription
        promise.reply(true);
    }

    public static void remove(String id, Promise<Boolean> promise){
        //Call Stripe to remove a subscription
        promise.reply(true);
    }
}
