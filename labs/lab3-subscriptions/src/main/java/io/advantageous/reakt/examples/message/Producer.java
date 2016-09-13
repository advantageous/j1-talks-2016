package io.advantageous.reakt.examples.message;

import kafka.admin.AdminUtils;
import kafka.utils.ZKStringSerializer$;
import org.I0Itec.zkclient.ZkClient;
import org.apache.kafka.clients.producer.*;
import java.util.Properties;

import static io.advantageous.reakt.examples.message.MessagingProperties.*;
import static org.apache.kafka.clients.producer.ProducerConfig.*;
/**
 * Created by jasondaniel on 9/12/16.
 */
public class Producer {
    private KafkaProducer<String, String> producer;
    private final int sessionTimeoutMs = 10 * 1000;
    private final int connectionTimeoutMs = 8 * 1000;

    public Producer(){
        topic();

        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaConnect);
        props.put(CLIENT_ID_CONFIG, clientId);
        props.put(KEY_SERIALIZER_CLASS_CONFIG, kafkaSerializationClass);
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, kafkaSerializationClass);

        producer = new KafkaProducer<>(props);
    }

    private void topic(){
        ZkClient zkClient = new ZkClient(
                zookeeperConnect,
                sessionTimeoutMs,
                connectionTimeoutMs,
                ZKStringSerializer$.MODULE$);

        if(!AdminUtils.topicExists(zkClient, topic)){
            System.out.println("Topic Doesn't Exist");
            AdminUtils.createTopic(zkClient, topic, partitions, replication, new Properties());
        }

        zkClient.close();
    }

    public void send(String key, String message, Callback callback){
        producer.send(new ProducerRecord<>(topic, key, message), callback);
    }

    public static void main(String[] args) throws InterruptedException {
        Producer producer = new Producer();
        producer.send(topic, "Test Message", (m, e) -> {
            long elapsedTime = System.currentTimeMillis();
            if (m != null) {
                System.out.println(
                        "message sent to partition(" + m.partition() +
                                "), " +
                                "offset(" + m.offset() + ") in " + elapsedTime + " ms");
            } else {
                e.printStackTrace();
            }
        });

        Thread.sleep(3000);
    }
}
