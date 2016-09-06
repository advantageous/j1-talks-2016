package io.advantageous.reakt.examples.service;

import io.advantageous.reakt.examples.model.Entitlement;
import io.advantageous.reakt.promise.Promise;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jasondaniel on 8/22/16.
 */
public interface EntitlementService {

    Promise<Boolean> create(Entitlement entitlement);

    Promise<Boolean> remove(String assetId, String subscriptionId);

    Promise<Entitlement> retrieve(String assetId, String subscriptionId);

    Promise<List<Entitlement>> list();
}
