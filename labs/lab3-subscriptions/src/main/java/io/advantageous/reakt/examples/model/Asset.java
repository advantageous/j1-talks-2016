package io.advantageous.reakt.examples.model;

import java.util.UUID;

/**
 * Created by jasondaniel on 8/22/16.
 */
public class Asset {
    private String id;
    private String name;
    private long createTime;

    public Asset(String id, String name, long createTime) {
        this.id = id;
        this.name = name;
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

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Asset)) return false;

        Asset asset = (Asset) o;

        if (createTime != asset.createTime) return false;
        if (id != null ? !id.equals(asset.id) : asset.id != null) return false;
        return name != null ? name.equals(asset.name) : asset.name == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (int) (createTime ^ (createTime >>> 32));
        return result;
    }
}
