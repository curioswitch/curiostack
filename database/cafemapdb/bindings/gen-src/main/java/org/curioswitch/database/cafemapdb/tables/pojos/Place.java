/*
 * This file is generated by jOOQ.
 */
package org.curioswitch.database.cafemapdb.tables.pojos;


import java.time.LocalDateTime;

import javax.annotation.processing.Generated;

import org.curioswitch.database.cafemapdb.tables.interfaces.IPlace;
import org.jooq.types.ULong;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.12.1"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Place implements IPlace {

    private static final long serialVersionUID = 1728634697;

    private final ULong         id;
    private final String        name;
    private final Double        latitude;
    private final Double        longitude;
    private final ULong         s2Cell;
    private final String        instagramId;
    private final String        googlePlaceId;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public Place(IPlace value) {
        this.id = value.getId();
        this.name = value.getName();
        this.latitude = value.getLatitude();
        this.longitude = value.getLongitude();
        this.s2Cell = value.getS2Cell();
        this.instagramId = value.getInstagramId();
        this.googlePlaceId = value.getGooglePlaceId();
        this.createdAt = value.getCreatedAt();
        this.updatedAt = value.getUpdatedAt();
    }

    public Place(
        ULong         id,
        String        name,
        Double        latitude,
        Double        longitude,
        ULong         s2Cell,
        String        instagramId,
        String        googlePlaceId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.s2Cell = s2Cell;
        this.instagramId = instagramId;
        this.googlePlaceId = googlePlaceId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Override
    public ULong getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Double getLatitude() {
        return this.latitude;
    }

    @Override
    public Double getLongitude() {
        return this.longitude;
    }

    @Override
    public ULong getS2Cell() {
        return this.s2Cell;
    }

    @Override
    public String getInstagramId() {
        return this.instagramId;
    }

    @Override
    public String getGooglePlaceId() {
        return this.googlePlaceId;
    }

    @Override
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    @Override
    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Place other = (Place) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        }
        else if (!id.equals(other.id))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        }
        else if (!name.equals(other.name))
            return false;
        if (latitude == null) {
            if (other.latitude != null)
                return false;
        }
        else if (!latitude.equals(other.latitude))
            return false;
        if (longitude == null) {
            if (other.longitude != null)
                return false;
        }
        else if (!longitude.equals(other.longitude))
            return false;
        if (s2Cell == null) {
            if (other.s2Cell != null)
                return false;
        }
        else if (!s2Cell.equals(other.s2Cell))
            return false;
        if (instagramId == null) {
            if (other.instagramId != null)
                return false;
        }
        else if (!instagramId.equals(other.instagramId))
            return false;
        if (googlePlaceId == null) {
            if (other.googlePlaceId != null)
                return false;
        }
        else if (!googlePlaceId.equals(other.googlePlaceId))
            return false;
        if (createdAt == null) {
            if (other.createdAt != null)
                return false;
        }
        else if (!createdAt.equals(other.createdAt))
            return false;
        if (updatedAt == null) {
            if (other.updatedAt != null)
                return false;
        }
        else if (!updatedAt.equals(other.updatedAt))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
        result = prime * result + ((this.latitude == null) ? 0 : this.latitude.hashCode());
        result = prime * result + ((this.longitude == null) ? 0 : this.longitude.hashCode());
        result = prime * result + ((this.s2Cell == null) ? 0 : this.s2Cell.hashCode());
        result = prime * result + ((this.instagramId == null) ? 0 : this.instagramId.hashCode());
        result = prime * result + ((this.googlePlaceId == null) ? 0 : this.googlePlaceId.hashCode());
        result = prime * result + ((this.createdAt == null) ? 0 : this.createdAt.hashCode());
        result = prime * result + ((this.updatedAt == null) ? 0 : this.updatedAt.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Place (");

        sb.append(id);
        sb.append(", ").append(name);
        sb.append(", ").append(latitude);
        sb.append(", ").append(longitude);
        sb.append(", ").append(s2Cell);
        sb.append(", ").append(instagramId);
        sb.append(", ").append(googlePlaceId);
        sb.append(", ").append(createdAt);
        sb.append(", ").append(updatedAt);

        sb.append(")");
        return sb.toString();
    }
}
