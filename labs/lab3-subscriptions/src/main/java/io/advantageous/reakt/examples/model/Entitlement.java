package io.advantageous.reakt.examples.model;

/**
 * Created by jasondaniel on 8/22/16.
 */
public class Entitlement {
    private String assetId;
    private String subscriptionId;
    private long createTime;

    public Entitlement(){}

    public Entitlement(String assetId, String subscriptionId, long createTime) {
        this.assetId = assetId;
        this.subscriptionId = subscriptionId;
        this.createTime = createTime;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
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
        if (!(o instanceof Entitlement)) return false;

        Entitlement that = (Entitlement) o;

        if (createTime != that.createTime) return false;
        if (assetId != null ? !assetId.equals(that.assetId) : that.assetId != null) return false;
        return subscriptionId != null ? subscriptionId.equals(that.subscriptionId) : that.subscriptionId == null;

    }

    @Override
    public int hashCode() {
        int result = assetId != null ? assetId.hashCode() : 0;
        result = 31 * result + (subscriptionId != null ? subscriptionId.hashCode() : 0);
        result = 31 * result + (int) (createTime ^ (createTime >>> 32));
        return result;
    }
}
