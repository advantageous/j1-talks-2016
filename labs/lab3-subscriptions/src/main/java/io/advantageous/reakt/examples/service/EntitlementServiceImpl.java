package io.advantageous.reakt.examples.service;

import io.advantageous.qbit.admin.ServiceManagementBundle;
import io.advantageous.qbit.annotation.PathVariable;
import io.advantageous.qbit.annotation.RequestMapping;
import io.advantageous.qbit.annotation.http.DELETE;
import io.advantageous.qbit.annotation.http.GET;
import io.advantageous.qbit.annotation.http.POST;
import io.advantageous.reakt.examples.model.Entitlement;
import io.advantageous.reakt.examples.repository.EntitlementRepository;
import io.advantageous.reakt.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

import static io.advantageous.reakt.promise.Promises.invokablePromise;

/**
 * Created by jasondaniel on 8/22/16.
 */
@RequestMapping("/entitlement-service")
public class EntitlementServiceImpl implements EntitlementService{
    private static final String PATH              = "/entitlement";
    private static final String ASSET_PATH        = "/asset";
    private static final String SUBSCRIPTION_PATH = "/subscription";
    private static final String HEARTBEAT_KEY     = "i.am.alive";
    private static final String MGMT_CREATE_KEY   = "entitlement.create.called";
    private static final String MGMT_REMOVE_KEY   = "entitlement.remove.called";
    private static final String MGMT_RETRIEVE_KEY = "entitlement.retrieve.called";
    private static final String MGMT_LIST_KEY     = "entitlement.list.called";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final EntitlementRepository repository;
    private final ServiceManagementBundle mgmt;

    public EntitlementServiceImpl(ServiceManagementBundle mgmt,
                                  EntitlementRepository repository){
        this.repository = repository;
        this.mgmt = mgmt;

        mgmt.reactor()
                .addRepeatingTask(Duration.ofSeconds(3),
                        () -> mgmt.increment(HEARTBEAT_KEY));
    }

    @Override
    @POST(value = PATH)
    public Promise<Boolean> create(final Entitlement entitlement) {
        return invokablePromise(promise -> {
            mgmt.increment(MGMT_CREATE_KEY);

            if(entitlement.getAssetId() == null){
                promise.reject("Asset Id required");
            }

            if(entitlement.getSubscriptionId() == null){
                promise.reject("Subscription Id required");
            }

            repository.store(entitlement)
                    .then(result -> {
                        logger.info("entitlement created");
                        promise.resolve(result);
                    })
                    .catchError(error -> {
                        logger.error("Unable to create entitlement", error);
                        promise.reject("Unable to create entitlement");
                    })
                    .invoke();
        });
    }


    @Override
    @DELETE(value = PATH+ASSET_PATH+"/{0}"+SUBSCRIPTION_PATH+"/{1}")
    public Promise<Boolean> remove(final @PathVariable String assetId,
                                   final @PathVariable String subscriptionId) {
        return invokablePromise(promise -> {
            mgmt.increment(MGMT_REMOVE_KEY);

            repository.remove(assetId, subscriptionId)
                    .then(entitlement -> {
                        logger.info("entitlement removed");
                        promise.resolve(true);
                    })
                    .catchError(error -> {
                        logger.error("Unable to remove entitlement", error);
                        promise.reject("Unable to remove entitlement");
                    })
                    .invoke();
        });
    }

    @Override
    @GET(value = PATH+ASSET_PATH+"/{0}"+SUBSCRIPTION_PATH+"/{1}")
    public Promise<Entitlement> retrieve(final @PathVariable String assetId,
                                         final @PathVariable String subscriptionId) {
        return invokablePromise(promise -> {
            mgmt.increment(MGMT_RETRIEVE_KEY);

            repository.find(assetId, subscriptionId)
                    .then(entitlement -> {
                        logger.info("entitlement retrieved");
                        promise.resolve(entitlement);
                    })
                    .catchError(error -> {
                        logger.error("Unable to find entitlement", error);
                        promise.reject("Unable to find entitlement");
                    })
                    .invoke();
        });
    }

    @Override
    @GET(value = PATH)
    public Promise<List<Entitlement>> list() {
        return invokablePromise(promise -> {
            mgmt.increment(MGMT_LIST_KEY);

            repository.list()
                    .then(entitlements -> {
                        logger.info("list entitlements");
                        promise.resolve(entitlements);
                    })
                    .catchError(error -> {
                        logger.error("Unable to list entitlements", error);
                        promise.reject("Unable to list entitlements");
                    })
                    .invoke();
        });
    }
}
