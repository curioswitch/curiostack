/*
 * This file is generated by jOOQ.
 */
package org.curioswitch.database.cafemapdb;


import javax.annotation.processing.Generated;

import org.curioswitch.database.cafemapdb.tables.FlywaySchemaHistory;
import org.curioswitch.database.cafemapdb.tables.Landmark;
import org.curioswitch.database.cafemapdb.tables.Place;


/**
 * Convenience access to all tables in cafemapdb
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.12.1"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Tables {

    /**
     * The table <code>cafemapdb.flyway_schema_history</code>.
     */
    public static final FlywaySchemaHistory FLYWAY_SCHEMA_HISTORY = FlywaySchemaHistory.FLYWAY_SCHEMA_HISTORY;

    /**
     * The table <code>cafemapdb.landmark</code>.
     */
    public static final Landmark LANDMARK = Landmark.LANDMARK;

    /**
     * The table <code>cafemapdb.place</code>.
     */
    public static final Place PLACE = Place.PLACE;
}
