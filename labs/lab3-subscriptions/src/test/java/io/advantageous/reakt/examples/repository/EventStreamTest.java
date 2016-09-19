package io.advantageous.reakt.examples.repository;

import io.advantageous.reakt.Stream;
import io.advantageous.reakt.examples.messaging.Consumer;
import io.advantageous.reakt.examples.messaging.Producer;
import io.advantageous.reakt.examples.model.Asset;
import io.advantageous.reakt.examples.model.Message;
import io.advantageous.reakt.examples.util.ConfigUtils;
import io.advantageous.test.DockerTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static io.advantageous.boon.json.JsonFactory.fromJson;
import static io.advantageous.boon.json.JsonFactory.toJson;
import static org.junit.Assert.assertEquals;

/**
 * Created by jasondaniel on 9/12/16.
 */

@Category(DockerTest.class)
public class EventStreamTest {
    private static Producer producer;
    private static Consumer consumer;
    private static String topic;


    @BeforeClass
    public static void setUp(){
        topic = ConfigUtils.getConfig("message")
                           .getConfig("kafka")
                           .getString("topic");

        producer = new Producer();
        consumer = new Consumer();

        consumer.consume(topic, result -> {
            Message message = fromJson(result.get(), Message.class);
            System.out.println(message.getMessage());
        }).invoke();
    }

    @AfterClass
    public static void shutDown(){
        consumer.shutdown();
    }


    @Test
    public void kafkaTest() throws InterruptedException {

        IntStream.range(0, 10).forEach(i -> {
            String json = toJson(new Message("test message "+i));
            producer.send("test-key", json).invoke();
        });

        Thread.sleep(3000);
    }


    //@Test
    public void testStream() throws Exception {
        TestStreamService testService = new TestStreamService();

        CountDownLatch countDownLatch = new CountDownLatch(3);
        AtomicLong counter = new AtomicLong();
        testService.streaming(result -> {
            System.out.println(result.get().getName());
            counter.incrementAndGet();
            countDownLatch.countDown();

        });

        countDownLatch.await();
        assertEquals(3L, counter.get());
    }

    public static class TestStreamService {

        public void streaming(final Stream<Asset> stream) {


            new Thread(() -> {
                stream.reply(new Asset(UUID.randomUUID().toString(), "Asset 1", System.currentTimeMillis()));
                sleep();
                stream.reply(new Asset(UUID.randomUUID().toString(), "Asset 2", System.currentTimeMillis()));
                sleep();
                stream.reply(new Asset(UUID.randomUUID().toString(), "Asset 3", System.currentTimeMillis()), true);
                sleep();
            }).start();
        }


        public void streamingWithCancel(final Stream<Asset> stream) {

            AtomicBoolean cancelled = new AtomicBoolean();

            new Thread(() -> {
                if (!cancelled.get()) {
                    stream.reply(new Asset(UUID.randomUUID().toString(), "Asset 1", System.currentTimeMillis()));
                }
                sleep();

                if (!cancelled.get()){
                    stream.reply(new Asset(UUID.randomUUID().toString(), "Asset 2", System.currentTimeMillis()), false, () -> cancelled.set(true), sendMore -> {});
                }
                sleep();
                if (!cancelled.get()) {
                    stream.reply(new Asset(UUID.randomUUID().toString(), "Asset 3", System.currentTimeMillis()), false, () -> cancelled.set(true));
                }
                sleep();
                sleep();
                if (!cancelled.get()) {
                    stream.reply(new Asset(UUID.randomUUID().toString(), "Asset 4", System.currentTimeMillis()), true, () -> cancelled.set(true));
                }
                sleep();
                sleep();
            }).start();
        }

        public void error(Stream<Asset> callback) {
            callback.fail("Error");
        }

        public void exception(Stream<Asset> callback) {
            callback.fail(new IllegalStateException("Error"));
        }

        private static void sleep() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
