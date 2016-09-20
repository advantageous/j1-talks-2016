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
        return invokablePromise(promise -> {
            mgmt.increment(MGMT_CREATE_KEY);

            //TODO  finish this
//            ThirdPartySubscriptionService.create(subscription)
//                    .then(thirdPartyId -> {
//                        logger.info(" subscription created id="+thirdPartyId);
//
//
//                        subscription.setThirdPartyId(thirdPartyId);
//
//
//                        repository.store(subscription)
//                                .then(result -> {
//                                    logger.info("subscription created id");
//                                    promise.resolve(result);
//                                })
//                                .catchError(error -> {
//                                    logger.error("Unable to create subscription", error);
//                                    promise.reject("Unable to create subscription");
//                                })
//                                .invoke();
//
//                    })
//                    .catchError(error -> {
//                        logger.error("Unable to create stripe subscription", error);
//                        promise.reject("Unable to create stripe subscription");
//                    }).invoke();

            // How could we track metrics about the repo store being successful?
            // How can we handle the callbacks in this same thread?

        });
    }

    @Override
    @PUT(value = PATH + "/{0}")
    public Promise<Boolean> update(final @PathVariable String id,
                                   final Subscription subscription) {
        return invokablePromise(promise -> {
            mgmt.increment(MGMT_UPDATE_KEY);

            subscription.setId(id);

            Promise<Boolean> repoPromise = repository.update(subscription)
                    .then(sub -> {
                        logger.info("subscription " + id + " updated");
                        promise.resolve(true);
                    })
                    .catchError(error -> {
                        logger.error("Unable to update subscription with id=" + id, error);
                        promise.reject("Unable to update subscription with id=" + id);
                    })
                    .invoke();

            Promise<Boolean> stripePromise = ThirdPartySubscriptionService.update(subscription)
                    .then(sub -> {
                        logger.info(" subscription " + id + " updated");
                        promise.resolve(true);
                    })
                    .catchError(error -> {
                        logger.error("Unable to update stripe subscription with id=" + id, error);
                        promise.reject("Unable to update stripe subscription with id=" + id);
                    })
                    .invoke();

            all(repoPromise, stripePromise);

            //TODO Advanced step find out what is wrong with this code?
            // What did we do wrong, and how would you fix it?
            // Why would this sometimes fail?


        });
    }

    @Override
    @DELETE(value = PATH + "/{0}")
    public Promise<Boolean> remove(final @PathVariable String id) {
        return invokablePromise(promise -> {
            mgmt.increment(MGMT_REMOVE_KEY);


            Promise<Boolean> repoPromise = repository.remove(id)
                    .then(subscription -> {
                        logger.info("subscription " + id + " removed");
                        promise.resolve(true);
                    })
                    .catchError(error -> {
                        logger.error("Unable to remove subscription with id=" + id, error);
                        promise.reject("Unable to remove subscription with id=" + id);
                    });

            Promise<Boolean> stripePromise = ThirdPartySubscriptionService.remove(id)
                    .then(subscription -> {
                        logger.info(" subscription " + id + " removed");
                        promise.resolve(true);
                    })
                    .catchError(error -> {
                        logger.error("Unable to remove stripe subscription with id=" + id, error);
                        promise.reject("Unable to remove stripe subscription with id=" + id);
                    });

            repoPromise.invoke();
            stripePromise.invoke();
            all(repoPromise, stripePromise);


            //TODO in Reakt 3.1, we made All promises invokeable. Change this to wrap the two calls
            // in an all promise and then invoke all of the promises by calling invoke with the invoke on
            // the all promise. You should no longer need to declare the promise variables.


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
