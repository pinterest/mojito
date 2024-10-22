CREATE TABLE scheduled_job (
    id CHAR(36) PRIMARY KEY,
    repository_id bigint NOT NULL,
    job_type smallint DEFAULT NULL,
    cron varchar(50) DEFAULT NULL,
    properties longtext NOT NULL,
    job_status smallint NOT NULL,
    start_date timestamp NULL DEFAULT NULL,
    end_date timestamp NULL DEFAULT NULL,
    enabled TINYINT DEFAULT 1
);

CREATE TABLE scheduled_job_type (
    id smallint NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name varchar(255) NOT NULL
);

CREATE TABLE scheduled_job_status_type (
    id smallint NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name varchar(255) NOT NULL
);

ALTER TABLE scheduled_job
    ADD UNIQUE KEY UK__REPOSITORY_ID__JOB_TYPE(repository_id, job_type);

alter table scheduled_job
    add constraint FK__SCHEDULED_JOB__IMPORT_REPOSITORY__ID
        foreign key (repository_id) references repository (id);

ALTER TABLE scheduled_job
    ADD CONSTRAINT FK__JOB_TYPE__JOB_TYPE_ID FOREIGN KEY (job_type) REFERENCES scheduled_job_type(id);

ALTER TABLE scheduled_job
    ADD CONSTRAINT FK__JOB_STATUS__JOB_STATUS_ID FOREIGN KEY (job_status) REFERENCES scheduled_job_status_type(id);


INSERT INTO scheduled_job_type(name) VALUES('THIRD_PARTY_SYNC');
INSERT INTO scheduled_job_status_type(name) VALUES('SCHEDULED'), ('IN_PROGRESS'), ('FAILED'), ('SUCCEEDED');

CREATE TABLE scheduled_job_aud (
    id CHAR(36) NOT NULL,
    rev integer NOT NULL,
    revtype tinyint,
    revend integer,
    repository_id bigint,
    job_type smallint DEFAULT NULL,
    cron varchar(50) DEFAULT NULL,
    properties longtext,
    job_status smallint,
    start_date timestamp NULL DEFAULT NULL,
    end_date timestamp NULL DEFAULT NULL,
    enabled TINYINT DEFAULT 1
);