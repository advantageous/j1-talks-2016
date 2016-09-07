package io.advantageous.reakt.examples.repository;

import io.advantageous.reakt.examples.model.Entitlement;
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
 * Created by jasondaniel on 9/6/16.
 */
@Category(DockerTest.class)
public class EntitlementRepoTest {

    private EntitlementRepository repository;
    private Entitlement entitlement;

    @Before
    public void before() throws Exception {
        String assetId = UUID.randomUUID().toString();
        String subscriptionId = UUID.randomUUID().toString();

        long createTime = System.currentTimeMillis();

        entitlement = new Entitlement(assetId, subscriptionId, createTime);


        repository = new EntitlementRepository(1, ConfigUtils.getConfig("entitlement")
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
        final Promise<Boolean> promise = repository.store(entitlement)
                .invokeAsBlockingPromise();

        assertTrue(promise.success());
        assertTrue(promise.get());
    }


    @Test
    public void testList() throws Exception {
        final Promise<List<Entitlement>> promise = repository.list().invokeAsBlockingPromise();

        assertTrue(promise.success());
        assertTrue(promise.get().size() > 0);
    }

    @Test
    public void testFind() throws Exception {
        String assetId = entitlement.getAssetId();
        String subscriptionId = entitlement.getSubscriptionId();

        repository.store(entitlement).invokeAsBlockingPromise();

        final Promise<Entitlement> findPromise = repository.find(assetId, subscriptionId).invokeAsBlockingPromise();
        assertTrue(findPromise.success());
        assertTrue(findPromise.get().getAssetId().equals(assetId));
        assertTrue(findPromise.get().getSubscriptionId().equals(subscriptionId));
    }

    @Test
    public void testRemove() throws Exception {
        String assetId = entitlement.getAssetId();
        String subscriptionId = entitlement.getSubscriptionId();

        repository.store(entitlement).invokeAsBlockingPromise();

        final Promise<Boolean> findPromise = repository.remove(assetId, subscriptionId).invokeAsBlockingPromise();

        assertTrue(findPromise.success());
        assertTrue(findPromise.get());
    }
}
