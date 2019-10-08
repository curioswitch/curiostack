/*
 * This file is generated by jOOQ.
 */
package org.curioswitch.database.cafemapdb;


import javax.annotation.processing.Generated;

import org.curioswitch.database.cafemapdb.tables.FlywaySchemaHistory;
import org.curioswitch.database.cafemapdb.tables.Landmark;
import org.curioswitch.database.cafemapdb.tables.Place;
import org.curioswitch.database.cafemapdb.tables.records.FlywaySchemaHistoryRecord;
import org.curioswitch.database.cafemapdb.tables.records.LandmarkRecord;
import org.curioswitch.database.cafemapdb.tables.records.PlaceRecord;
import org.jooq.Identity;
import org.jooq.UniqueKey;
import org.jooq.impl.Internal;
import org.jooq.types.ULong;


/**
 * A class modelling foreign key relationships and constraints of tables of 
 * the <code>cafemapdb</code> schema.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.12.1"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

    // -------------------------------------------------------------------------
    // IDENTITY definitions
    // -------------------------------------------------------------------------

    public static final Identity<LandmarkRecord, ULong> IDENTITY_LANDMARK = Identities0.IDENTITY_LANDMARK;
    public static final Identity<PlaceRecord, ULong> IDENTITY_PLACE = Identities0.IDENTITY_PLACE;

    // -------------------------------------------------------------------------
    // UNIQUE and PRIMARY KEY definitions
    // -------------------------------------------------------------------------

    public static final UniqueKey<FlywaySchemaHistoryRecord> KEY_FLYWAY_SCHEMA_HISTORY_PRIMARY = UniqueKeys0.KEY_FLYWAY_SCHEMA_HISTORY_PRIMARY;
    public static final UniqueKey<LandmarkRecord> KEY_LANDMARK_PRIMARY = UniqueKeys0.KEY_LANDMARK_PRIMARY;
    public static final UniqueKey<LandmarkRecord> KEY_LANDMARK_GOOGLE_PLACE_ID = UniqueKeys0.KEY_LANDMARK_GOOGLE_PLACE_ID;
    public static final UniqueKey<PlaceRecord> KEY_PLACE_PRIMARY = UniqueKeys0.KEY_PLACE_PRIMARY;
    public static final UniqueKey<PlaceRecord> KEY_PLACE_IDX_INSTAGRAM_ID = UniqueKeys0.KEY_PLACE_IDX_INSTAGRAM_ID;

    // -------------------------------------------------------------------------
    // FOREIGN KEY definitions
    // -------------------------------------------------------------------------


    // -------------------------------------------------------------------------
    // [#1459] distribute members to avoid static initialisers > 64kb
    // -------------------------------------------------------------------------

    private static class Identities0 {
        public static Identity<LandmarkRecord, ULong> IDENTITY_LANDMARK = Internal.createIdentity(Landmark.LANDMARK, Landmark.LANDMARK.ID);
        public static Identity<PlaceRecord, ULong> IDENTITY_PLACE = Internal.createIdentity(Place.PLACE, Place.PLACE.ID);
    }

    private static class UniqueKeys0 {
        public static final UniqueKey<FlywaySchemaHistoryRecord> KEY_FLYWAY_SCHEMA_HISTORY_PRIMARY = Internal.createUniqueKey(FlywaySchemaHistory.FLYWAY_SCHEMA_HISTORY, "KEY_flyway_schema_history_PRIMARY", FlywaySchemaHistory.FLYWAY_SCHEMA_HISTORY.INSTALLED_RANK);
        public static final UniqueKey<LandmarkRecord> KEY_LANDMARK_PRIMARY = Internal.createUniqueKey(Landmark.LANDMARK, "KEY_landmark_PRIMARY", Landmark.LANDMARK.ID);
        public static final UniqueKey<LandmarkRecord> KEY_LANDMARK_GOOGLE_PLACE_ID = Internal.createUniqueKey(Landmark.LANDMARK, "KEY_landmark_google_place_id", Landmark.LANDMARK.GOOGLE_PLACE_ID);
        public static final UniqueKey<PlaceRecord> KEY_PLACE_PRIMARY = Internal.createUniqueKey(Place.PLACE, "KEY_place_PRIMARY", Place.PLACE.ID);
        public static final UniqueKey<PlaceRecord> KEY_PLACE_IDX_INSTAGRAM_ID = Internal.createUniqueKey(Place.PLACE, "KEY_place_idx_instagram_id", Place.PLACE.INSTAGRAM_ID);
    }
}
