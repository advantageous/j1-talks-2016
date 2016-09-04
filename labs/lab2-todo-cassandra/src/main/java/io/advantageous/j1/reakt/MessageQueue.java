package io.advantageous.j1.reakt;

import io.advantageous.reakt.promise.Promise;
import io.advantageous.reakt.promise.Promises;

public class MessageQueue {

    /** This is just an example. No Kafka or animals were hurt in this example. */
    public Promise<Boolean> sendToQueue (final Todo todo) {
        return Promises.invokablePromise(p -> {});
    }

}
