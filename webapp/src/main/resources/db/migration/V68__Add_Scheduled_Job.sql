CREATE TABLE scheduled_job (
    id bigint AUTO_INCREMENT PRIMARY KEY,
    repository_id bigint NOT NULL,
    job_type VARCHAR(255),
    cron VARCHAR(15),
    properties longtext NOT NULL,
    job_status VARCHAR(255) NOT NULL,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE scheduled_job
    ADD UNIQUE KEY UK__REPOSITORY_ID__JOB_TYPE (repository_id, job_type);

alter table `scheduled_job`
    add constraint FK__SCHEDULED_JOB__IMPORT_REPOSITORY__ID
        foreign key (repository_id) references repository (id);