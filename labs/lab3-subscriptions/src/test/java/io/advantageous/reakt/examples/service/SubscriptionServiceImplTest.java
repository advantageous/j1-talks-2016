package io.advantageous.reakt.examples.service;

import io.advantageous.boon.core.Sys;
import io.advantageous.qbit.admin.ManagedServiceBuilder;
import io.advantageous.qbit.admin.ServiceManagementBundle;
import io.advantageous.qbit.queue.QueueCallBackHandler;
import io.advantageous.qbit.service.ServiceBuilder;
import io.advantageous.reakt.examples.model.Subscription;
import io.advantageous.reakt.examples.repository.SubscriptionRepository;
import io.advantageous.reakt.promise.Promise;
import io.advantageous.reakt.promise.Promises;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.advantageous.qbit.admin.ManagedServiceBuilder.managedServiceBuilder;
import static io.advantageous.qbit.admin.ServiceManagementBundleBuilder.serviceManagementBundleBuilder;
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

public class SubscriptionServiceImplTest {

    //@Test
    public void testSubscriptionServiceImpl() throws Exception {
        final SubscriptionService subscriptionService = createSubscriptionService();

        final Subscription sub1 = new Subscription(UUID.randomUUID().toString(), "sub1", "");
        final Subscription sub2 = new Subscription(UUID.randomUUID().toString(), "sub2", "");

        assertTrue(subscriptionService.create(sub1).invoke().get());
        assertTrue(subscriptionService.create(sub2).invoke().get());
        assertTrue(subscriptionService.remove(sub1.getId()).invoke().get());
        assertTrue(subscriptionService.list()
                .invoke()
                .get()
                .stream()
                .filter(
                        sub -> sub.getName().equals("sub2")
                )
                .findFirst()
                .isPresent()
        );


        assertFalse(subscriptionService.list()
                .invoke()
                .get()
                .stream()
                .filter(
                        sub -> sub.getName().equals("sub1")

                )
                .findFirst()
                .isPresent()
        );
    }


    private SubscriptionService createSubscriptionService() {
        final ManagedServiceBuilder managedServiceBuilder = managedServiceBuilder();

        final ServiceManagementBundle serviceManagementBundle =
                serviceManagementBundleBuilder().setServiceName("SubscriptionServiceImpl")
                        .setManagedServiceBuilder(managedServiceBuilder).build();

        final SubscriptionService subscriptionService = new SubscriptionServiceImpl(serviceManagementBundle);


        return ServiceBuilder.serviceBuilder().setServiceObject(subscriptionService).addQueueCallbackHandler(
                new QueueCallBackHandler() {
                    @Override
                    public void queueProcess() {
                        serviceManagementBundle.process();
                    }
                })
                .buildAndStartAll()
                .createProxyWithAutoFlush(SubscriptionService.class, 50, TimeUnit.MILLISECONDS);

    }
}
