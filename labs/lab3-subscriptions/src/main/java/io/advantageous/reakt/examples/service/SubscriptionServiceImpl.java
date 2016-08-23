package io.advantageous.reakt.examples.service;

import io.advantageous.qbit.admin.ManagedServiceBuilder;
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
import io.advantageous.reakt.promise.Promises;

import java.net.URI;
import java.time.Duration;
import java.util.*;

import static io.advantageous.qbit.admin.ManagedServiceBuilder.managedServiceBuilder;
import static io.advantageous.qbit.admin.ServiceManagementBundleBuilder.serviceManagementBundleBuilder;
import static io.advantageous.reakt.promise.Promises.*;
/**
 * Created by jasondaniel on 8/11/16.
 */

@RequestMapping("/subscription-service")
public class SubscriptionServiceImpl implements SubscriptionService {
    private static final int PORT                 = 8082;
    private static final String VERSION           = "/v1";
    private static final String PATH              = "/subscription";
    private static final String HEARTBEAT_KEY     = "i.am.alive";
    private static final String MGMT_CREATE_KEY   = "subscription.create.called";
    private static final String MGMT_UPDATE_KEY   = "subscription.update.called";
    private static final String MGMT_REMOVE_KEY   = "subscription.remove.called";
    private static final String MGMT_RETRIEVE_KEY = "subscription.retrieve.called";
    private static final String MGMT_LIST_KEY     = "subscription.list.called";
    private static final String STATSD_ADDRESS    = "udp://192.168.99.100:8125";


    private final SubscriptionRepository repository;
    private final ServiceManagementBundle mgmt;

    public SubscriptionServiceImpl(ServiceManagementBundle mgmt){
        repository = new SubscriptionRepository();

        this.mgmt = mgmt;

        mgmt.reactor()
            .addRepeatingTask(Duration.ofSeconds(3),
                    () -> mgmt.increment(HEARTBEAT_KEY));
    }

    @Override
    @POST(value = PATH)
    public Promise<Boolean> create(final Subscription subscription) {
        return invokablePromise(completePromise -> {
            mgmt.increment(MGMT_CREATE_KEY);

            Promise<String> stripePromise = promise();
            Promise<Boolean> subscriptionPromise = promise();

            all(stripePromise, subscriptionPromise);

            StripeService.create(stripePromise);

            subscription.setId(UUID.randomUUID().toString());
            subscription.setThirdPartyId(stripePromise.get());

            repository.store(subscription, subscriptionPromise);

            completePromise.accept(true);
        });
    }

    @Override
    @PUT(value = PATH+"/{0}")
    public Promise<Boolean> update(final @PathVariable String id,
                                   final Subscription subscription) {
        return invokablePromise(completePromise -> {
            mgmt.increment(MGMT_UPDATE_KEY);

            Promise<Subscription> findPromise = promise();
            Promise<Boolean> updatePromise = promise();
            Promise<Boolean> updateStripePromise = promise();

            all(updatePromise, updateStripePromise);

            repository.find(id, findPromise);

            Subscription current = findPromise.get();

            if(current == null){
                current = new Subscription();
                current.setId(id);
            }

            if(subscription.getName() != null){
                current.setName(subscription.getName());
            }

            repository.update(current, updatePromise);
            StripeService.update(current, Promises.promise());

            completePromise.accept(true);
        });
    }

    @Override
    @DELETE(value = PATH+"/{0}")
    public Promise<Boolean> remove(final @PathVariable String id) {
        return invokablePromise(completePromise -> {
            mgmt.increment(MGMT_REMOVE_KEY);

            Promise<Boolean> removePromise = promise();
            Promise<Boolean> removeStripePromise = promise();

            all(removePromise, removeStripePromise);

            repository.remove(id, promise());
            StripeService.remove(id, removeStripePromise);

            completePromise.accept(true);
        });
    }

    @Override
    @GET(value = PATH+"/{0}")
    public Promise<Subscription> retrieve(final @PathVariable String id) {
        return invokablePromise(completePromise -> {
            mgmt.increment(MGMT_RETRIEVE_KEY);

            Promise<Subscription> findPromise = promise();
            repository.find(id, findPromise);
            completePromise.accept(findPromise.get());
        });
    }

    @Override
    @GET(value = PATH)
    public Promise<List<Subscription>> list() {
        return invokablePromise(completePromise -> {
            mgmt.increment(MGMT_LIST_KEY);

            Promise<List<Subscription>> listPromise = promise();
            repository.list(listPromise);
            completePromise.accept(listPromise.get());
        });
    }

    public static void main(final String... args) throws Exception {

        final ManagedServiceBuilder managedServiceBuilder = managedServiceBuilder()
                .setRootURI(VERSION)
                .setPort(PORT)
                .enableStatsD(URI.create(STATSD_ADDRESS));

        managedServiceBuilder.getContextMetaBuilder()
                             .setTitle(SubscriptionServiceImpl.class.getSimpleName());

        final ServiceManagementBundle serviceManagementBundle = serviceManagementBundleBuilder()
                .setServiceName(SubscriptionServiceImpl.class.getSimpleName())
                .setManagedServiceBuilder(managedServiceBuilder).build();

        final SubscriptionService subscriptionService = new SubscriptionServiceImpl(serviceManagementBundle);

        managedServiceBuilder
                .addEndpointServiceWithServiceManagmentBundle(subscriptionService, serviceManagementBundle)
                .startApplication();

        managedServiceBuilder.getAdminBuilder().build().startServer();

        System.out.println("Subscription Server and Admin Server started");

    }
}
