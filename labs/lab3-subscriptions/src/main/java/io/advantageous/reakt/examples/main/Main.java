package io.advantageous.reakt.examples.main;

import io.advantageous.config.Config;
import io.advantageous.qbit.admin.ManagedServiceBuilder;
import io.advantageous.qbit.admin.ServiceManagementBundle;
import io.advantageous.reakt.examples.repository.AssetRepository;
import io.advantageous.reakt.examples.repository.EntitlementRepository;
import io.advantageous.reakt.examples.repository.SubscriptionRepository;
import io.advantageous.reakt.examples.service.*;
import io.advantageous.reakt.examples.util.ConfigUtils;

import java.net.URI;

import static io.advantageous.qbit.admin.ManagedServiceBuilder.managedServiceBuilder;
import static io.advantageous.qbit.admin.ServiceManagementBundleBuilder.serviceManagementBundleBuilder;

/**
 * Created by jasondaniel on 9/1/16.
 */
public class Main {
    private static final int PORT                  = 8082;
    private static final String VERSION            = "/v1";
    private static final String STATSD_ADDRESS     = "udp://192.168.99.100:8125";
    private static final String REPLICATION_FACTOR = "cassandra.replicationFactor";
    private static final String URIS               = "cassandra.uris";


    public static void main(final String... args) throws Exception {
        final Config config = ConfigUtils.getConfig("subscription");

        final ManagedServiceBuilder managedServiceBuilder = managedServiceBuilder()
                .setRootURI(VERSION)
                .setPort(PORT)
                .enableStatsD(URI.create(STATSD_ADDRESS));

        managedServiceBuilder.getContextMetaBuilder()
                .setTitle(SubscriptionServiceImpl.class.getSimpleName());

        final ServiceManagementBundle serviceManagementBundle = serviceManagementBundleBuilder()
                .setServiceName(SubscriptionServiceImpl.class.getSimpleName())
                .setManagedServiceBuilder(managedServiceBuilder).build();

        final SubscriptionRepository subscriptionRepository =
                new SubscriptionRepository(config.getInt(REPLICATION_FACTOR),
                        config.getUriList(URIS));

        final SubscriptionService subscriptionService =
                new SubscriptionServiceImpl(serviceManagementBundle, subscriptionRepository);

        final AssetRepository assetRepository =
                new AssetRepository(config.getInt(REPLICATION_FACTOR),
                        config.getUriList(URIS));

        final AssetService assetService =
                new AssetServiceImpl(serviceManagementBundle, assetRepository);

        final EntitlementRepository entitlementRepository =
                new EntitlementRepository(config.getInt(REPLICATION_FACTOR),
                        config.getUriList(URIS));

        final EntitlementService entitlementService =
                new EntitlementServiceImpl(serviceManagementBundle, entitlementRepository);

        final MessageService messageService =
                new MessageServiceImpl(serviceManagementBundle);

        managedServiceBuilder
                .addEndpointServiceWithServiceManagmentBundle(subscriptionService, serviceManagementBundle)
                .addEndpointServiceWithServiceManagmentBundle(assetService, serviceManagementBundle)
                .addEndpointServiceWithServiceManagmentBundle(entitlementService, serviceManagementBundle)
                .addEndpointServiceWithServiceManagmentBundle(messageService, serviceManagementBundle)
                .startApplication();

        managedServiceBuilder.getAdminBuilder().build().startServer();

        System.out.println("Server and Admin Server started");

    }
}
