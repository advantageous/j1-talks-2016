package io.advantageous.reakt.examples.service;

import io.advantageous.reakt.examples.model.Entitlement;
import io.advantageous.reakt.promise.Promise;

import java.util.ArrayList;

/**
 * Created by jasondaniel on 8/22/16.
 */
public interface EntitlementService {

    Promise<Boolean> create(Entitlement subscription);

    Promise<Boolean> update(String id, Entitlement subscription);

    Promise<Boolean> remove(String id);

    Promise<Entitlement> retrieve(String id);

    Promise<ArrayList<Entitlement>> list();
}
