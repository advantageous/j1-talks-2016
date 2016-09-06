package io.advantageous.reakt.examples.service;

import io.advantageous.reakt.examples.model.Asset;
import io.advantageous.reakt.promise.Promise;

import java.util.List;

/**
 * Created by jasondaniel on 9/6/16.
 */
public interface AssetService {

    Promise<Boolean> create(Asset asset);

    Promise<Boolean> update(String id, Asset asset);

    Promise<Boolean> remove(String id);

    Promise<Asset> retrieve(String id);

    Promise<List<Asset>> list();
}
