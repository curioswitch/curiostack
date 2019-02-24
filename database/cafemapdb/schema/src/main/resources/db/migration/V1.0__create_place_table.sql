CREATE TABLE place
(
  id           BIGINT UNSIGNED AUTO_INCREMENT
    PRIMARY KEY,
  name         VARCHAR(255) NOT NULL,
  latitude     DOUBLE       NOT NULL,
  longitude    DOUBLE       NOT NULL,
  instagram_id VARCHAR(255) NULL
)
