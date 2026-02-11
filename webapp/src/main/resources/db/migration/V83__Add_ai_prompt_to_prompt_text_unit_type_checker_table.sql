CREATE TABLE ai_prompt_to_prompt_text_unit_type_checker (
    ai_prompt_id bigint(20) not null,
    prompt_text_unit_type_checker varchar(8) not null,
    primary key (ai_prompt_id)
);

ALTER TABLE ai_prompt_to_prompt_text_unit_type_checker ADD CONSTRAINT FK__AI_PROMPT_TO_TU_TYPE_CHECKER__AI_PROMPT__ID foreign key (ai_prompt_id) references ai_prompt (id) on delete cascade;
