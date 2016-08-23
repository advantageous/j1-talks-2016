package io.advantageous.reakt.examples.service;

import io.advantageous.qbit.admin.ManagedServiceBuilder;
import io.advantageous.qbit.admin.ServiceManagementBundle;
import io.advantageous.qbit.annotation.PathVariable;
import io.advantageous.qbit.annotation.http.DELETE;
import io.advantageous.qbit.annotation.http.GET;
import io.advantageous.qbit.annotation.http.POST;
import io.advantageous.qbit.annotation.http.PUT;
import io.advantageous.reakt.examples.model.Entitlement;
import io.advantageous.reakt.examples.model.Subscription;
import io.advantageous.reakt.promise.Promise;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import static io.advantageous.qbit.admin.ManagedServiceBuilder.managedServiceBuilder;
import static io.advantageous.qbit.admin.ServiceManagementBundleBuilder.serviceManagementBundleBuilder;
import static io.advantageous.reakt.promise.Promises.invokablePromise;

/**
 * Created by jasondaniel on 8/22/16.
 */
public class EntitlementServiceImpl implements EntitlementService{
    private static final int PORT                 = 8082;
    private static final String VERSION           = "/v1";
    private static final String PATH              = "/entitlement";
    private static final String HEARTBEAT_KEY     = "i.am.alive";
    private static final String MGMT_CREATE_KEY   = "entitlement.create.called";
    private static final String MGMT_UPDATE_KEY   = "entitlement.update.called";
    private static final String MGMT_REMOVE_KEY   = "entitlement.remove.called";
    private static final String MGMT_RETRIEVE_KEY = "entitlement.retrieve.called";
    private static final String MGMT_LIST_KEY     = "entitlement.list.called";
    private static final String STATSD_ADDRESS    = "udp://192.168.99.100:8125";


    private final Map<String, Entitlement> map = new TreeMap<>();
    private final ServiceManagementBundle mgmt;

    public EntitlementServiceImpl(ServiceManagementBundle mgmt){
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
            map.put(entitlement.getAsset().getId()+
                    entitlement.getSubscription().getId(), entitlement);
            promise.accept(true);
        });
    }

    @Override
    @PUT(value = PATH+"/{0}")
    public Promise<Boolean> update(final @PathVariable String id,
                                   final Entitlement entitlement) {
        return invokablePromise(promise -> {
            mgmt.increment(MGMT_UPDATE_KEY);

            Entitlement current = map.get(id);

            if(current == null){
                current = new Entitlement();
            }

            if(entitlement.getSubscription() != null){
                current.setSubscription(entitlement.getSubscription());
            }

            if(entitlement.getAsset() != null){
                current.setAsset(entitlement.getAsset());
            }

            map.put(id, current);
            promise.accept(true);
        });
    }

    @Override
    @DELETE(value = PATH+"/{0}")
    public Promise<Boolean> remove(final @PathVariable String id) {
        return invokablePromise(promise -> {
            mgmt.increment(MGMT_REMOVE_KEY);
            map.remove(id);
            promise.accept(true);
        });
    }

    @Override
    @GET(value = PATH+"/{0}")
    public Promise<Entitlement> retrieve(final @PathVariable String id) {
        return invokablePromise(promise -> {
            mgmt.increment(MGMT_RETRIEVE_KEY);
            promise.accept(map.get(id));
        });
    }

    @Override
    @GET(value = PATH)
    public Promise<ArrayList<Entitlement>> list() {
        return invokablePromise(promise -> {
            mgmt.increment(MGMT_LIST_KEY);
            promise.accept(new ArrayList<>(map.values()));
        });
    }

    /*
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
    */
}
