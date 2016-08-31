package io.advantageous.j1.reakt;


import io.advantageous.config.Config;
import io.advantageous.discovery.DiscoveryService;
import io.advantageous.j1.reakt.repo.TodoRepo;
import io.advantageous.j1.reakt.repo.TodoRepoImpl;
import io.advantageous.qbit.admin.ManagedServiceBuilder;
import io.advantageous.qbit.admin.ServiceManagementBundle;
import io.advantageous.qbit.service.ServiceQueue;

import java.time.Duration;

import static io.advantageous.qbit.admin.ManagedServiceBuilder.managedServiceBuilder;
import static io.advantageous.qbit.admin.ServiceManagementBundleBuilder.serviceManagementBundleBuilder;


public class Main {

    public static void main(final String... args) throws Exception {


        final Config config = ConfigUtils.getConfig("todo");
        final ManagedServiceBuilder managedServiceBuilder = createManagedServiceBuilder(config);
        final TodoRepo todoRepoProxy = createTodoRepo(config, managedServiceBuilder, false);


        /** Create the management bundle for this service. */
        final ServiceManagementBundle serviceManagementBundle =
                serviceManagementBundleBuilder().setServiceName("TodoServiceImpl")
                        .setManagedServiceBuilder(managedServiceBuilder).build();

        serviceManagementBundle.addServicesToFlush(todoRepoProxy);

        final TodoService todoService = new TodoServiceImpl(serviceManagementBundle, todoRepoProxy);

        /* Start the service. */
        managedServiceBuilder
                //Register TodoServiceImpl
                .addEndpointServiceWithServiceManagmentBundle(todoService, serviceManagementBundle)
                //Build and start the server.
                .startApplication();

        /* Start the admin builder which exposes health end-points and swagger meta data. */
        managedServiceBuilder.getAdminBuilder()
                .setPort(9090)
                .build().startServer();

        System.out.println("Todo Server and Admin Server started");

    }

    public static ManagedServiceBuilder createManagedServiceBuilder(Config config) {
    /* Create the ManagedServiceBuilder which manages a clean shutdown, health, stats, etc. */
        final ManagedServiceBuilder managedServiceBuilder = managedServiceBuilder()
                .setRootURI("/v1") //Defaults to services
                .setPort(8081); //Defaults to 8080 or environment variable PORT

        managedServiceBuilder.enableLoggingMappedDiagnosticContext();
        //URI.create("udp://grafana.marathon.mesos:12103")
        managedServiceBuilder.enableStatsD(config.getUri("statsd"));
        managedServiceBuilder.getContextMetaBuilder().setTitle("TodoMicroService");
        return managedServiceBuilder;
    }

    public static TodoRepo createTodoRepo(final Config config,
                                          final ManagedServiceBuilder managedServiceBuilder,
                                          final boolean autoFlush) {
        final ServiceManagementBundle repoServiceMgmt =
                serviceManagementBundleBuilder().setServiceName("TodoRepo")
                        .setManagedServiceBuilder(managedServiceBuilder).build();

        final TodoRepoImpl todoRepo = new TodoRepoImpl(
                config.getInt("cassandra.replicationFactor"),
                config.getUri("cassandra.uri"), repoServiceMgmt,
                DiscoveryService.create(config.getUriList("discoveryURIs")));


        final ServiceQueue serviceQueue = managedServiceBuilder
                .createServiceBuilderForServiceObject(todoRepo).addQueueProcessListener(() -> repoServiceMgmt.process())
                .buildAndStartAll();
        return autoFlush ?
                serviceQueue.createProxyWithAutoFlush(TodoRepo.class, Duration.ofMillis(10))
                : serviceQueue.createProxy(TodoRepo.class);

    }
}
