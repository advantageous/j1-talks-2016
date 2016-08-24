package io.advantageous.reakt.examples.service;

import io.advantageous.reakt.examples.model.Subscription;
import io.advantageous.reakt.promise.Promise;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jasondaniel on 8/11/16.
 */
public interface SubscriptionService {

    Promise<Subscription> create(Subscription subscription);

    Promise<Boolean> update(String id, Subscription subscription);

    Promise<Boolean> remove(String id);

    Promise<Subscription> retrieve(String id);

    Promise<List<Subscription>> list();
}
