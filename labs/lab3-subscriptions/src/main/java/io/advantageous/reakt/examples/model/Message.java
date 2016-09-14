package io.advantageous.reakt.examples.model;

import io.advantageous.boon.json.JsonFactory;

import java.util.UUID;


/**
 * Created by jasondaniel on 9/14/16.
 */
public class Message {
    private String id;
    private String message;
    private long createTime;


    public Message(){
        id = UUID.randomUUID().toString();
        createTime = System.currentTimeMillis();
    }

    public Message(String message){
        id = UUID.randomUUID().toString();
        createTime = System.currentTimeMillis();
        this.message = message;
    }

    public String getId() {
        if (id == null) {
            this.id = UUID.randomUUID().toString();
        }
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message)) return false;

        Message message1 = (Message) o;

        if (createTime != message1.createTime) return false;
        if (id != null ? !id.equals(message1.id) : message1.id != null) return false;
        return message != null ? message.equals(message1.message) : message1.message == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (int) (createTime ^ (createTime >>> 32));
        return result;
    }
}
