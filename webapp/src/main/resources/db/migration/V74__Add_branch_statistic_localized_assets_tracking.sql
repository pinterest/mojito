ALTER TABLE branch_statistic
    ADD COLUMN in_localized_asset BIT DEFAULT 0 NOT NULL AFTER branch_id,
    ADD COLUMN localized_asset_commit BIGINT DEFAULT NULL AFTER in_localized_asset;