package io.advantageous.reakt.examples.service;

import io.advantageous.qbit.admin.ServiceManagementBundle;
import io.advantageous.qbit.annotation.PathVariable;
import io.advantageous.qbit.annotation.RequestMapping;
import io.advantageous.qbit.annotation.http.DELETE;
import io.advantageous.qbit.annotation.http.GET;
import io.advantageous.qbit.annotation.http.POST;
import io.advantageous.qbit.annotation.http.PUT;
import io.advantageous.reakt.examples.model.Subscription;
import io.advantageous.reakt.examples.repository.SubscriptionRepository;
import io.advantageous.reakt.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

import static io.advantageous.reakt.promise.Promises.all;
import static io.advantageous.reakt.promise.Promises.invokablePromise;

/**
 * Created by jasondaniel on 8/11/16.
 */

@RequestMapping("/subscription-service")
public class SubscriptionServiceImpl implements SubscriptionService {
    private static final String PATH = "/subscription";
    private static final String HEARTBEAT_KEY = "i.am.alive";
    private static final String MGMT_CREATE_KEY = "subscription.create.called";
    private static final String MGMT_UPDATE_KEY = "subscription.update.called";
    private static final String MGMT_REMOVE_KEY = "subscription.remove.called";
    private static final String MGMT_RETRIEVE_KEY = "subscription.retrieve.called";
    private static final String MGMT_LIST_KEY = "subscription.list.called";
    private final ThirdPartySubscriptionService thirdPartySubscriptionService = new ThirdPartySubscriptionService();

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final SubscriptionRepository repository;
    private final ServiceManagementBundle mgmt;

    public SubscriptionServiceImpl(ServiceManagementBundle mgmt,
                                   SubscriptionRepository repository) {
        this.repository = repository;
        this.mgmt = mgmt;

        mgmt.reactor()
                .addRepeatingTask(Duration.ofSeconds(3),
                        () -> mgmt.increment(HEARTBEAT_KEY));
    }

    @Override
    @POST(value = PATH)
    public Promise<Boolean> create(final Subscription subscription) {
        return invokablePromise(returnPromise -> {
            mgmt.increment(MGMT_CREATE_KEY);

            thirdPartySubscriptionService.create(subscription)
                    .then(thirdPartyId -> {
                        logger.info("Subscription created id=" + thirdPartyId);
                        subscription.setThirdPartyId(thirdPartyId);
                        repository.store(subscription)
                                .then(result -> {
                                    logger.info("subscription created id");
                                    returnPromise.resolve(result);
                                })
                                .catchError(error -> {
                                    logger.error("Unable to create subscription", error);
                                    returnPromise.reject("Unable to create subscription");
                                })
                                .invoke();
                    })
                    .catchError(error -> {
                        logger.error("Unable to create stripe subscription", error);
                        returnPromise.reject("Unable to create stripe subscription");
                    });
        });
    }

    @Override
    @PUT(value = PATH + "/{0}")
    public Promise<Boolean> update(final @PathVariable String id,
                                   final Subscription subscription) {
        return invokablePromise(returnPromise -> {
            mgmt.increment(MGMT_UPDATE_KEY);

            subscription.setId(id);

            //Create all promise, do this operations
            all(
                    //Update returns promise
                    repository.update(subscription)
                            .then(sub -> logger.info("subscription " + id + " updated"))
                            .catchError(error -> {
                                logger.error("Unable to update subscription with id=" + id, error);
                                returnPromise.reject("Unable to update subscription with id=" + id);
                            }),
                    //3rd party returns promise
                    thirdPartySubscriptionService.update(subscription)
                            .then(sub -> logger.info("Subscription " + id + " updated"))
                            .catchError(error -> {
                                logger.error("Unable to update stripe subscription with id=" + id, error);
                                returnPromise.reject("Unable to update stripe subscription with id=" + id);
                            })

            )
                    //Then resolve calling promise
                    .then(v -> returnPromise.resolve(true))
                    //Or resolve error by rejecting calling promise.
                    .catchError(error -> returnPromise.reject("Unable to do update", error))
                    .invoke();
        });
    }

    @Override
    @DELETE(value = PATH + "/{0}")
    public Promise<Boolean> remove(final @PathVariable String id) {
        return invokablePromise(returnPromise -> {
            mgmt.increment(MGMT_REMOVE_KEY);

            Promise<Boolean> repoPromise = repository.remove(id)
                    .then(subscription -> {
                        logger.info("subscription " + id + " removed");
                    })
                    .catchError(error -> {
                        logger.error("Unable to remove subscription with id=" + id, error);
                    });

            Promise<Boolean> subscriptionPromise = thirdPartySubscriptionService.remove(id)
                    .then(subscription -> {
                        logger.info("Stripe subscription " + id + " removed");
                    })
                    .catchError(error -> {
                        logger.error("Unable to remove stripe subscription with id=" + id, error);
                    });

            all(repoPromise, subscriptionPromise)
                    //Then resolve calling promise
                    .then(v -> returnPromise.resolve(true))
                    //Or resolve error by rejecting calling promise.
                    .catchError(error -> returnPromise.reject("Unable to do update", error));

            repoPromise.invoke();
            subscriptionPromise.invoke();

        });
    }

    @Override
    @GET(value = PATH + "/{0}")
    public Promise<Subscription> retrieve(final @PathVariable String id) {
        return invokablePromise(promise -> {
            mgmt.increment(MGMT_RETRIEVE_KEY);

            repository.find(id)
                    .then(subscription -> {
                        logger.info("subscription " + id + " retrieved");
                        promise.resolve(subscription);
                    })
                    .catchError(error -> {
                        logger.error("Unable to find subscription with id=" + id, error);
                        promise.reject("Unable to find subscription with id=" + id);
                    })
                    .invoke();
        });
    }

    @Override
    @GET(value = PATH)
    public Promise<List<Subscription>> list() {
        return invokablePromise(promise -> {
            mgmt.increment(MGMT_LIST_KEY);

            repository.list()
                    .then(subscriptions -> {
                        logger.info("list subscriptions");
                        promise.resolve(subscriptions);
                    })
                    .catchError(error -> {
                        logger.error("Unable to list subscriptions", error);
                        promise.reject("Unable to list subscriptions");
                    })
                    .invoke();
        });
    }
}
