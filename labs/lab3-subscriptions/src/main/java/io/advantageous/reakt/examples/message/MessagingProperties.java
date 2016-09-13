package io.advantageous.reakt.examples.message;

/**
 * Created by jasondaniel on 9/13/16.
 */
public class MessagingProperties {
    public static final String zookeeperConnect = "192.168.99.100:2181";
    public static final String kafkaConnect = "192.168.99.100:9092";
    public static final String groupId = "group1";
    public static final String topic = "my-topic";
    public static final String clientId = "ReaktProducer";
    public static final String kafkaSerializationClass = "org.apache.kafka.common.serialization.StringSerializer";
    public static final int partitions = 2;
    public static final int replication = 1;

}
