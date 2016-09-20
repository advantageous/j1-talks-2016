package io.advantageous.reakt.examples.service;

import io.advantageous.reakt.examples.model.Message;
import io.advantageous.reakt.promise.Promise;

import java.util.List;

/**
 * Created by jasondaniel on 9/14/16.
 */
public interface MessageService {

    Promise<Boolean> publish(Message message);

}
