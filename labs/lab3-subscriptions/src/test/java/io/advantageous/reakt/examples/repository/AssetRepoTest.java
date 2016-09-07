package io.advantageous.reakt.examples.repository;

import io.advantageous.reakt.examples.model.Asset;
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
public class AssetRepoTest {

    private AssetRepository repository;
    private Asset asset;

    @Before
    public void before() throws Exception {
        String id = UUID.randomUUID().toString();
        String name = "test asset";
        long createTime = System.currentTimeMillis();

        asset = new Asset(id, name, createTime);


        repository = new AssetRepository(1, ConfigUtils.getConfig("asset")
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
        final Promise<Boolean> promise = repository.store(asset)
                .invokeAsBlockingPromise();

        assertTrue(promise.success());
        assertTrue(promise.get());
    }

    @Test
    public void testUpdate() throws Exception {
        repository.store(asset)
                .invokeAsBlockingPromise();

        asset.setName("updated test asset");

        final Promise<Boolean> promise = repository.update(asset)
                .invokeAsBlockingPromise();

        assertTrue(promise.success());
        assertTrue(promise.get());
    }

    @Test
    public void testList() throws Exception {
        final Promise<List<Asset>> promise = repository.list().invokeAsBlockingPromise();

        assertTrue(promise.success());
        assertTrue(promise.get().size() > 0);
    }

    @Test
    public void testFind() throws Exception {
        String id = asset.getId();

        repository.store(asset).invokeAsBlockingPromise();

        final Promise<Asset> findPromise = repository.find(id).invokeAsBlockingPromise();
        assertTrue(findPromise.success());
        assertTrue(findPromise.get().getId().equals(id));
    }

    @Test
    public void testRemove() throws Exception {
        String id = asset.getId();
        repository.store(asset).invokeAsBlockingPromise();

        final Promise<Boolean> findPromise = repository.remove(id).invokeAsBlockingPromise();

        assertTrue(findPromise.success());
        assertTrue(findPromise.get());
    }
}
