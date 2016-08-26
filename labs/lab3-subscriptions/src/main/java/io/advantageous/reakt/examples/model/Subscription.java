package io.advantageous.reakt.examples.model;

import java.util.UUID;

/**
 * Created by jasondaniel on 8/11/16.
 */
public class Subscription {
    private String id;
    private String name;
    private String thirdPartyId;
    private long createTime;

    public Subscription(){}

    public Subscription(String id, String name, String thirdPartyId) {
        this.id = id;
        this.name = name;
        this.thirdPartyId = thirdPartyId;
    }

    public Subscription(String id, String name, String thirdPartyId, long createTime) {
        this.id = id;
        this.name = name;
        this.thirdPartyId = thirdPartyId;
        this.createTime = createTime;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getThirdPartyId() {
        return thirdPartyId;
    }

    public void setThirdPartyId(String thirdPartyId) {
        this.thirdPartyId = thirdPartyId;
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
        if (!(o instanceof Subscription)) return false;

        Subscription that = (Subscription) o;

        if (createTime != that.createTime) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return thirdPartyId != null ? thirdPartyId.equals(that.thirdPartyId) : that.thirdPartyId == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (thirdPartyId != null ? thirdPartyId.hashCode() : 0);
        result = 31 * result + (int) (createTime ^ (createTime >>> 32));
        return result;
    }
}

