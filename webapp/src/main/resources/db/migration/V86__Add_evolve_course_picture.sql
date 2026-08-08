CREATE TABLE evolve_course_picture (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    course_id     INT          NOT NULL,
    locale_bcp47_tag VARCHAR(20) NOT NULL,
    picture_url   VARCHAR(2048),
    hero_picture_url VARCHAR(2048),
    hero_picture_mobile_url VARCHAR(2048),
    PRIMARY KEY (id),
    CONSTRAINT UK__EVOLVE_COURSE_PICTURE__COURSE_ID__LOCALE_BCP47_TAG
        UNIQUE (course_id, locale_bcp47_tag)
);
