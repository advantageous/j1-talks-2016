package io.advantageous.reakt.examples.service;

import io.advantageous.qbit.admin.ServiceManagementBundle;
import io.advantageous.qbit.annotation.RequestMapping;
import io.advantageous.qbit.annotation.http.POST;
import io.advantageous.reakt.examples.messaging.Consumer;
import io.advantageous.reakt.examples.messaging.Producer;
import io.advantageous.reakt.examples.model.Message;
import io.advantageous.reakt.examples.util.ConfigUtils;
import io.advantageous.reakt.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

import static io.advantageous.boon.json.JsonFactory.fromJson;
import static io.advantageous.boon.json.JsonFactory.toJson;
import static io.advantageous.reakt.promise.Promises.invokablePromise;

/**
 * Created by jasondaniel on 9/14/16.
 */
@RequestMapping("/message-service")
public class MessageServiceImpl implements MessageService {
    private static final String PATH              = "/message";
    private static final String HEARTBEAT_KEY     = "i.am.alive";
    private static final String MGMT_PUBLISH_KEY   = "message.publish.called";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ServiceManagementBundle mgmt;
    private String topic;
    private Producer producer;
    private Consumer consumer;

    public MessageServiceImpl(ServiceManagementBundle mgmt){
        topic = ConfigUtils.getConfig("message")
                           .getConfig("kafka")
                           .getString("topic");

        producer = new Producer();
        consumer = new Consumer();

        this.mgmt = mgmt;
        mgmt.reactor()
                .addRepeatingTask(Duration.ofSeconds(3),
                        () -> mgmt.increment(HEARTBEAT_KEY));

        startConsumer();
    }

    @Override
    @POST(value = PATH)
    public Promise<Boolean> publish(final Message message) {
        return invokablePromise(promise -> {
//       TODO send message to Kafka using our publisher
//            mgmt.increment(MGMT_PUBLISH_KEY);
//            producer send("message-key" // convert message to JSON toJson(message))
                    //then(???)
                    //catchError(???)
                    //???invoke();
            // What would you do differently if you wanted to track stats?
            // How would you use the Reakt Reactor?
        });
    }

    private void startConsumer(){
        consumer.consume(topic, result -> {
            Message message = fromJson(result.get(), Message.class);
            logger.info(message.getMessage());
        }).invoke();
    }
}
