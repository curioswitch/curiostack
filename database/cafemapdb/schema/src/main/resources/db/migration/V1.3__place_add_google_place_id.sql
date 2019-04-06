ALTER TABLE place
    ADD COLUMN google_place_id VARCHAR(255) NOT NULL DEFAULT '' AFTER instagram_id;
