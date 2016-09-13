package io.advantageous.reakt.examples.message;

import kafka.consumer.ConsumerConfig;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.advantageous.reakt.examples.message.MessagingProperties.*;

/**
 * Created by jasondaniel on 9/13/16.
 */
public class Consumer {
    private final ExecutorService executor;
    private final ConsumerConnector consumer;

    public Consumer() {

        executor = Executors.newFixedThreadPool(2);
        consumer = kafka.consumer
                        .Consumer
                        .createJavaConsumerConnector(config());
    }

    public void consume(String topic) {
        KafkaStream<byte[], byte[]> stream =
                consumer.createMessageStreams(
                    new HashMap<String, Integer>(){
                        {
                            put(topic, 1);
                        }
                    }
                ).get(topic).get(0);

        executor.submit(() -> stream.forEach(
                data -> System.out.println("Consumer Received " + new String(data.message()))));

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

    private static ConsumerConfig config(){
        Properties props = new Properties();
        props.put("zookeeper.connect", zookeeperConnect);
        props.put("group.id", groupId);
        props.put("zookeeper.session.timeout.ms", "400");
        props.put("zookeeper.sync.time.ms", "200");
        props.put("auto.commit.interval.ms", "1000");
        props.put("auto.offset.reset", "smallest");

        return new ConsumerConfig(props);
    }


    public static void main(String[] args) throws InterruptedException {
        Consumer consumer = new Consumer();
        consumer.consume(topic);
    }
}
