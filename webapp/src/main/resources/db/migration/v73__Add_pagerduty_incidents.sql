CREATE TABLE pagerduty_incidents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    client_name VARCHAR(255) NOT NULL,
    dedup_key VARCHAR(255) NOT NULL,
    triggered_at DATETIME NULL NOT NULL,
    resolved_at DATETIME NULL DEFAULT NULL
);
