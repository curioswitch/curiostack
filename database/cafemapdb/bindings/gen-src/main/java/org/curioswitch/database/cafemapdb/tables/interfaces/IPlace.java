/*
 * This file is generated by jOOQ.
 */
package org.curioswitch.database.cafemapdb.tables.interfaces;


import java.io.Serializable;
import java.time.LocalDateTime;

import javax.annotation.processing.Generated;

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
public interface IPlace extends Serializable {

    /**
     * Getter for <code>cafemapdb.place.id</code>.
     */
    public ULong getId();

    /**
     * Getter for <code>cafemapdb.place.name</code>.
     */
    public String getName();

    /**
     * Getter for <code>cafemapdb.place.latitude</code>.
     */
    public Double getLatitude();

    /**
     * Getter for <code>cafemapdb.place.longitude</code>.
     */
    public Double getLongitude();

    /**
     * Getter for <code>cafemapdb.place.s2_cell</code>.
     */
    public ULong getS2Cell();

    /**
     * Getter for <code>cafemapdb.place.instagram_id</code>.
     */
    public String getInstagramId();

    /**
     * Getter for <code>cafemapdb.place.google_place_id</code>.
     */
    public String getGooglePlaceId();

    /**
     * Getter for <code>cafemapdb.place.created_at</code>.
     */
    public LocalDateTime getCreatedAt();

    /**
     * Getter for <code>cafemapdb.place.updated_at</code>.
     */
    public LocalDateTime getUpdatedAt();
}
