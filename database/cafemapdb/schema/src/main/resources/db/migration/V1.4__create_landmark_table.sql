CREATE TABLE landmark
(
    id              BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    google_place_id VARCHAR(255) NOT NULL UNIQUE,
    s2_cell         VARCHAR(255) NOT NULL,
    type            VARCHAR(255) NOT NULL
)
