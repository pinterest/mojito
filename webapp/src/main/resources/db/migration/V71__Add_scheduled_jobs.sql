CREATE TABLE scheduled_job (
    id CHAR(36) PRIMARY KEY NOT NULL,
    repository_id BIGINT NOT NULL,
    job_type SMALLINT DEFAULT NULL,
    cron VARCHAR(50) DEFAULT NULL,
    properties LONGTEXT NOT NULL,
    job_status SMALLINT NOT NULL,
    start_date TIMESTAMP NULL DEFAULT NULL,
    end_date TIMESTAMP NULL DEFAULT NULL,
    enabled TINYINT DEFAULT 1
);

CREATE TABLE scheduled_job_type (
    id SMALLINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE scheduled_job_status_type (
    id SMALLINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

ALTER TABLE scheduled_job
    ADD UNIQUE KEY UK__REPOSITORY_ID__JOB_TYPE(repository_id, job_type);

ALTER TABLE scheduled_job
    ADD CONSTRAINT FK__SCHEDULED_JOB__IMPORT_REPOSITORY__ID
        FOREIGN KEY (repository_id) REFERENCES repository (id);

ALTER TABLE scheduled_job
    ADD CONSTRAINT FK__JOB_TYPE__JOB_TYPE_ID FOREIGN KEY (job_type) REFERENCES scheduled_job_type(id);

ALTER TABLE scheduled_job
    ADD CONSTRAINT FK__JOB_STATUS__JOB_STATUS_ID FOREIGN KEY (job_status) REFERENCES scheduled_job_status_type(id);


INSERT INTO scheduled_job_type(name) VALUES('THIRD_PARTY_SYNC');
INSERT INTO scheduled_job_status_type(name) VALUES('SCHEDULED'), ('IN_PROGRESS'), ('FAILED'), ('SUCCEEDED');

CREATE TABLE scheduled_job_aud (
    id CHAR(36) NOT NULL,
    rev INTEGER NOT NULL,
    revtype TINYINT,
    revend INTEGER,
    repository_id BIGINT,
    job_type SMALLINT DEFAULT NULL,
    cron VARCHAR(50) DEFAULT NULL,
    properties LONGTEXT,
    job_status SMALLINT,
    start_date TIMESTAMP NULL DEFAULT NULL,
    end_date TIMESTAMP NULL DEFAULT NULL,
    enabled TINYINT DEFAULT 1
);