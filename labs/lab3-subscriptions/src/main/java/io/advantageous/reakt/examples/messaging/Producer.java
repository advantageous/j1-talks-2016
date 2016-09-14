package io.advantageous.reakt.examples.messaging;

import io.advantageous.config.Config;
import io.advantageous.reakt.examples.util.ConfigUtils;
import io.advantageous.reakt.promise.Promise;
import kafka.admin.AdminUtils;
import kafka.utils.ZKStringSerializer$;
import org.I0Itec.zkclient.ZkClient;
import org.apache.kafka.clients.producer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Properties;
import static io.advantageous.reakt.promise.Promises.invokablePromise;
import static org.apache.kafka.clients.producer.ProducerConfig.*;
/**
 * Created by jasondaniel on 9/12/16.
 */
public class Producer {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private KafkaProducer<String, String> producer;
    private int sessionTimeoutMs = 10 * 1000;
    private int connectionTimeoutMs = 8 * 1000;
    private int partitions;
    private int replication;
    private String kafkaConnect;
    private String zookeeperConnect;
    private String topic;
    private String clientId;
    private String kafkaSerializationClass;

    public Producer(){
        config();
        topic();

        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaConnect);

        props.put(CLIENT_ID_CONFIG,  clientId);
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


    public Promise<Boolean> send(String key, String message){
        return invokablePromise(promise -> {
            producer.send(new ProducerRecord<>(topic, key, message), (m, e) -> {
                if (m != null) {

                    logger.info("message "+message+" sent to partition(" + m.partition() + "), " +
                            "offset(" + m.offset() + ") at " + System.currentTimeMillis());

                    promise.resolve(true);
                } else {
                    promise.reject(e);
                }
            });
        });

    }

    private void config() {
        Config config = ConfigUtils.getConfig("message").getConfig("kafka");

        kafkaConnect = config.getString("kafkaConnect");
        clientId = config.getString("clientId");
        kafkaSerializationClass = config.getString("kafkaSerializationClass");
        zookeeperConnect = config.getString("zookeeperConnect");
        topic = config.getString("topic");
        partitions = config.getInt("partitions");
        replication = config.getInt("replication");
    }
}
