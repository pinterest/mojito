create table tm_text_unit_variant_integrity_check_failure (
    id bigint(20) not null auto_increment,
    tm_text_unit_id bigint not null,
    locale_id bigint not null,
    tm_text_unit_variant_id bigint not null,
    integrity_failure_name varchar(255),
    created_date datetime,
    primary key (id)
);

alter table tm_text_unit_variant_integrity_check_failure add constraint FK__TM_TUV_ICF__TM_TEXT_UNIT__ID foreign key (tm_text_unit_id) references tm_text_unit (id);
alter table tm_text_unit_variant_integrity_check_failure add constraint FK__TM_TUV_ICF__LOCALE__ID foreign key (locale_id) references locale (id);
alter table tm_text_unit_variant_integrity_check_failure add constraint FK__TM_TUV_ICF__TM_TEXT_UNIT_VARIANT__ID foreign key (tm_text_unit_variant_id) references tm_text_unit_variant (id);
