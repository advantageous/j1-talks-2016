package io.advantageous.dcos;


import io.advantageous.config.Config;
import io.advantageous.qbit.admin.ManagedServiceBuilder;
import io.advantageous.qbit.admin.ServiceManagementBundle;

import java.net.URI;

import static io.advantageous.qbit.admin.ManagedServiceBuilder.managedServiceBuilder;
import static io.advantageous.qbit.admin.ServiceManagementBundleBuilder.serviceManagementBundleBuilder;


public class Main {

    public static void main(final String... args) throws Exception {


        final Config config = ConfigUtils.getConfig("todo");

        /* Create the ManagedServiceBuilder which manages a clean shutdown, health, stats, etc. */
        final ManagedServiceBuilder managedServiceBuilder = managedServiceBuilder()
                .setRootURI("/v1") //Defaults to services
                .setPort(8081); //Defaults to 8080 or environment variable PORT

        //URI.create("udp://grafana.marathon.mesos:12103")
        managedServiceBuilder.enableStatsD(config.getUri("statsd"));
        managedServiceBuilder.getContextMetaBuilder().setTitle("TodoMicroService");

        /** Create the management bundle for this service. */
        final ServiceManagementBundle serviceManagementBundle =
                serviceManagementBundleBuilder().setServiceName("TodoServiceImpl")
                        .setManagedServiceBuilder(managedServiceBuilder).build();

        final TodoService todoService = new TodoServiceImpl(serviceManagementBundle,
                new TodoRepo(config.getInt("cassandra.replicationFactor"),
                        config.getUriList("cassandra.uris")));

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
}
