package io.advantageous.reakt.examples.model;

/**
 * Created by jasondaniel on 8/22/16.
 */
public class Entitlement {
    private Asset asset;
    private Subscription subscription;

    public Entitlement(){}

    public Entitlement(Asset asset, Subscription subscription) {
        this.asset = asset;
        this.subscription = subscription;
    }

    public Asset getAsset() {
        return asset;
    }

    public void setAsset(Asset asset) {
        this.asset = asset;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Entitlement)) return false;

        Entitlement that = (Entitlement) o;

        if (asset != null ? !asset.equals(that.asset) : that.asset != null) return false;
        return subscription != null ? subscription.equals(that.subscription) : that.subscription == null;

    }

    @Override
    public int hashCode() {
        int result = asset != null ? asset.hashCode() : 0;
        result = 31 * result + (subscription != null ? subscription.hashCode() : 0);
        return result;
    }
}
