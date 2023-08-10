create table third_party_sync_file_checksum (repository_id bigint not null, locale_id bigint not null, checksum char(32) not null, file_name varchar(255) not null);
alter table third_party_sync_file_checksum add constraint FK__THIRD_PARTY_CHECKSUM__REPO__ID foreign key (repository_id) references repository (id);
alter table third_party_sync_file_checksum add constraint FK__THIRD_PARTY_CHECKSUM__LOCALE__ID foreign key (locale_id) references locale (id);
create index I__THIRD_PARTY__CHECKSUM__REPOSITORY_ID__FILE_NAME__LOCALE_ID on third_party_sync_file_checksum(repository_id, locale_id, file_name);