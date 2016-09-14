package io.advantageous.reakt.examples.service;

import io.advantageous.reakt.examples.model.Message;
import io.advantageous.reakt.promise.Promise;

import java.util.List;

/**
 * Created by jasondaniel on 9/14/16.
 */
public interface MessageService {

    Promise<Boolean> publish(Message message);
    /*

    Promise<Boolean> publish(Message message, int count);

    Promise<String> publish(Message message, long interval);

    Promise<Message> get(String messageId);

    Promise<List<Message>> list();

    Promise<Boolean> cancel(String messageId);
    */

}
