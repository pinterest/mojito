CREATE TABLE tm_text_unit_to_branch(
   id bigint AUTO_INCREMENT PRIMARY KEY,
   tm_text_unit_id bigint NOT NULL,
   branch_id bigint NOT NULL
);

ALTER TABLE tm_text_unit_to_branch
ADD CONSTRAINT FK__TM_TEXT_UNIT_ID__INTRODUCED_IN FOREIGN KEY (tm_text_unit_id) REFERENCES tm_text_unit(id);

ALTER TABLE tm_text_unit_to_branch
ADD CONSTRAINT FK__BRANCH_ID__INTRODUCED_IN FOREIGN KEY (branch_id) REFERENCES branch(id);


CREATE TABLE branch_source(
   id bigint AUTO_INCREMENT PRIMARY KEY,
   branch_id bigint NOT NULL,
   url VARCHAR(300) NOT NULL
);

ALTER TABLE branch_source
ADD CONSTRAINT FK__BRANCH_ID__BRANCH_SOURCE_URL FOREIGN KEY (branch_id) REFERENCES branch(id);

