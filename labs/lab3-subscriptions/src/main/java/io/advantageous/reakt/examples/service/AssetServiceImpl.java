package io.advantageous.reakt.examples.service;

import io.advantageous.qbit.admin.ServiceManagementBundle;
import io.advantageous.qbit.annotation.PathVariable;
import io.advantageous.qbit.annotation.RequestMapping;
import io.advantageous.qbit.annotation.http.DELETE;
import io.advantageous.qbit.annotation.http.GET;
import io.advantageous.qbit.annotation.http.POST;
import io.advantageous.qbit.annotation.http.PUT;
import io.advantageous.reakt.examples.model.Asset;
import io.advantageous.reakt.examples.repository.AssetRepository;
import io.advantageous.reakt.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

import static io.advantageous.reakt.promise.Promises.invokablePromise;

/**
 * Created by jasondaniel on 9/6/16.
 */
@RequestMapping("/asset-service")
public class AssetServiceImpl implements AssetService {

    private static final String PATH              = "/asset";
    private static final String HEARTBEAT_KEY     = "i.am.alive";
    private static final String MGMT_CREATE_KEY   = "asset.create.called";
    private static final String MGMT_UPDATE_KEY   = "asset.update.called";
    private static final String MGMT_REMOVE_KEY   = "asset.remove.called";
    private static final String MGMT_RETRIEVE_KEY = "asset.retrieve.called";
    private static final String MGMT_LIST_KEY     = "asset.list.called";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final AssetRepository repository;
    private final ServiceManagementBundle mgmt;

    public AssetServiceImpl(ServiceManagementBundle mgmt,
                                   AssetRepository repository){
        this.repository = repository;
        this.mgmt = mgmt;

        mgmt.reactor()
                .addRepeatingTask(Duration.ofSeconds(3),
                        () -> mgmt.increment(HEARTBEAT_KEY));
    }

    @Override
    @POST(value = PATH)
    public Promise<Boolean> create(Asset asset) {
        return invokablePromise(promise -> {
            mgmt.increment(MGMT_CREATE_KEY);

            repository.store(asset)
                    .then(result -> {
                        logger.info("asset created");
                        promise.resolve(result);
                    })
                    .catchError(error -> {
                        logger.error("Unable to create asset", error);
                        promise.reject("Unable to create asset");
                    })
                    .invoke();

        });
    }

    @Override
    @PUT(value = PATH+"/{0}")
    public Promise<Boolean> update(final @PathVariable String id, Asset asset) {
        return invokablePromise(promise -> {
            mgmt.increment(MGMT_UPDATE_KEY);

            asset.setId(id);

            repository.update(asset)
                    .then(sub -> {
                        logger.info("asset "+id+" updated");
                        promise.resolve(true);
                    })
                    .catchError(error -> {
                        logger.error("Unable to update asset with id="+id, error);
                        promise.reject("Unable to update asset with id="+id);
                    })
                    .invoke();
        });
    }

    @Override
    @DELETE(value = PATH+"/{0}")
    public Promise<Boolean> remove(final @PathVariable String id) {
        return invokablePromise(promise -> {
            mgmt.increment(MGMT_REMOVE_KEY);

            repository.remove(id)
                    .then(subscription -> {
                        logger.info("asset "+id+" removed");
                        promise.resolve(true);
                    })
                    .catchError(error -> {
                        logger.error("Unable to remove asset with id="+id, error);
                        promise.reject("Unable to remove asset with id="+id);
                    })
                    .invoke();
        });
    }

    @Override
    @GET(value = PATH+"/{0}")
    public Promise<Asset> retrieve(final @PathVariable String id) {
        return invokablePromise(promise -> {
            mgmt.increment(MGMT_RETRIEVE_KEY);

            repository.find(id)
                    .then(subscription -> {
                        logger.info("asset "+id+" retrieved");
                        promise.resolve(subscription);
                    })
                    .catchError(error -> {
                        logger.error("Unable to find asset with id="+id, error);
                        promise.reject("Unable to find asset with id="+id);
                    })
                    .invoke();
        });
    }

    @Override
    @GET(value = PATH)
    public Promise<List<Asset>> list() {
        return invokablePromise(promise -> {
            mgmt.increment(MGMT_LIST_KEY);

            repository.list()
                    .then(subscriptions -> {
                        logger.info("list assets");
                        promise.resolve(subscriptions);
                    })
                    .catchError(error -> {
                        logger.error("Unable to list assets", error);
                        promise.reject("Unable to list assets");
                    })
                    .invoke();
        });
    }
}
