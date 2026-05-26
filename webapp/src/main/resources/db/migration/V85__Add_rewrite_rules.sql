CREATE TABLE rewrite_rules (
    id BIGINT NOT NULL AUTO_INCREMENT,
    repository_id BIGINT,
    locale_id BIGINT NOT NULL,
    rewrite_from VARCHAR(512) NOT NULL,
    rewrite_to VARCHAR(512) NOT NULL,
    enabled BIT NOT NULL,
    created_date DATETIME NOT NULL,
    last_modified_date DATETIME,
    created_by_user_id BIGINT NOT NULL,
    repository_id_scope BIGINT GENERATED ALWAYS AS (IFNULL(repository_id, 0)) STORED,
    active_rewrite_from VARCHAR(512)
        GENERATED ALWAYS AS (CASE WHEN enabled = 1 THEN rewrite_from ELSE NULL END) STORED,
    PRIMARY KEY(id)
);

ALTER TABLE rewrite_rules
    ADD CONSTRAINT FK__REWRITE_RULES__REPOSITORY__ID
        FOREIGN KEY (repository_id) REFERENCES repository (id);

ALTER TABLE rewrite_rules
    ADD CONSTRAINT FK__REWRITE_RULES__LOCALE__ID
        FOREIGN KEY (locale_id) REFERENCES locale (id);

ALTER TABLE rewrite_rules
   ADD CONSTRAINT FK__REWRITE_RULES__USER__ID
    FOREIGN KEY (created_by_user_id) REFERENCES user (id);

CREATE UNIQUE INDEX I__REWRITE_RULES__REPO_ID_SCOPE__LOCALE_ID__ACTIVE_REWRITE_FROM
    ON rewrite_rules (repository_id_scope, locale_id, active_rewrite_from);