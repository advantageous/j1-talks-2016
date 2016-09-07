package io.advantageous.reakt.examples.repository;

import io.advantageous.reakt.examples.model.Subscription;
import io.advantageous.reakt.examples.util.ConfigUtils;
import io.advantageous.reakt.promise.Promise;
import io.advantageous.test.DockerTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

/**
 * Created by jasondaniel on 8/24/16.
 */
@Category(DockerTest.class)
public class SubscriptionRepoTest {
    private SubscriptionRepository repository;
    private Subscription subscription;

    @Before
    public void before() throws Exception {
        String id = UUID.randomUUID().toString();
        String thirdPartyId = UUID.randomUUID().toString();
        String name = "test subscription";
        long createTime = System.currentTimeMillis();

        subscription = new Subscription(id, name, thirdPartyId, createTime);


        repository = new SubscriptionRepository(1, ConfigUtils.getConfig("subscription")
                                                              .getConfig("cassandra")
                                                              .getUriList("uris"));

        repository.connect().invokeAsBlockingPromise().get();
        Thread.sleep(1000);
    }

    @After
    public void after() throws Exception {
        repository.close();
    }

    @Test
    public void testStore() throws Exception {
        final Promise<Boolean> promise = repository.store(subscription)
                                                   .invokeAsBlockingPromise();

        assertTrue(promise.success());
        assertTrue(promise.get());
    }

    @Test
    public void testUpdate() throws Exception {
        repository.store(subscription)
                  .invokeAsBlockingPromise();

        subscription.setName("updated test subscription");

        final Promise<Boolean> promise = repository.update(subscription)
                .invokeAsBlockingPromise();

        assertTrue(promise.success());
        assertTrue(promise.get());
    }

    @Test
    public void testList() throws Exception {
        final Promise<List<Subscription>> promise = repository.list().invokeAsBlockingPromise();

        assertTrue(promise.success());
        assertTrue(promise.get().size() > 0);
    }

    @Test
    public void testFind() throws Exception {
        String id = subscription.getId();

        repository.store(subscription).invokeAsBlockingPromise();

        final Promise<Subscription> findPromise = repository.find(id).invokeAsBlockingPromise();
        assertTrue(findPromise.success());
        assertTrue(findPromise.get().getId().equals(id));
    }

    @Test
    public void testRemove() throws Exception {
        String id = subscription.getId();

        repository.store(subscription).invokeAsBlockingPromise();

        final Promise<Boolean> findPromise = repository.remove(id).invokeAsBlockingPromise();

        assertTrue(findPromise.success());
        assertTrue(findPromise.get());
    }
}
