package io.advantageous.reakt.examples.messaging;

import io.advantageous.config.Config;
import io.advantageous.reakt.Stream;
import io.advantageous.reakt.examples.util.ConfigUtils;
import io.advantageous.reakt.promise.Promise;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.advantageous.reakt.promise.Promises.invokablePromise;

/**
 * Created by jasondaniel on 9/13/16.
 */
public class Consumer {
    private final ExecutorService executor;
    private final ConsumerConnector consumer;
    private static String zookeeperConnect;
    private static String groupId;
    private int partitions;


    public Consumer() {
        config();

        executor = Executors.newFixedThreadPool(partitions);
        consumer = kafka.consumer
                        .Consumer
                        .createJavaConsumerConnector(ConsumerConfig());
    }

    public Promise<Boolean> consume(String topic, Stream<String> stream) {
        return invokablePromise(promise -> {
            KafkaStream<byte[], byte[]> kafkaStream =
                    consumer.createMessageStreams(
                            new HashMap<String, Integer>(){
                                {
                                    put(topic, 1);
                                }
                            }
                    ).get(topic).get(0);

            executor.submit(() -> kafkaStream.forEach(
                    data -> stream.reply(new String(data.message()))));
        });
    }

    public void shutdown() {
        if (consumer != null) consumer.shutdown();
        if (executor != null) executor.shutdown();
        try {
            if (!executor.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
                System.out.println("Timed out waiting for consumer threads to shut down, exiting uncleanly");
            }
        } catch (InterruptedException e) {
            System.out.println("Interrupted during shutdown, exiting uncleanly");
        }
    }

    private static ConsumerConfig ConsumerConfig(){
        Properties props = new Properties();
        props.put("zookeeper.connect", zookeeperConnect);
        props.put("group.id", groupId);
        props.put("zookeeper.session.timeout.ms", "400");
        props.put("zookeeper.sync.time.ms", "200");
        props.put("auto.commit.interval.ms", "1000");
        props.put("auto.offset.reset", "smallest");

        return new ConsumerConfig(props);
    }

    private void config() {
        Config config = ConfigUtils.getConfig("message").getConfig("kafka");

        zookeeperConnect = config.getString("zookeeperConnect");
        partitions = config.getInt("partitions");
        groupId = config.getString("groupId");
    }
}
