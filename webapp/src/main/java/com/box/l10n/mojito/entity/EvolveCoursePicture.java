package com.box.l10n.mojito.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "evolve_course_picture",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "UK__EVOLVE_COURSE_PICTURE__COURSE_ID__LOCALE_BCP47_TAG",
          columnNames = {"course_id", "locale_bcp47_tag"})
    })
public class EvolveCoursePicture extends BaseEntity {

  @Column(name = "course_id", nullable = false)
  private int courseId;

  @Column(name = "locale_bcp47_tag", nullable = false, length = 20)
  private String localeBcp47Tag;

  @Column(name = "picture_url")
  private String pictureUrl;

  @Column(name = "hero_picture_url")
  private String heroPictureUrl;

  @Column(name = "hero_picture_mobile_url")
  private String heroPictureMobileUrl;

  public int getCourseId() {
    return courseId;
  }

  public void setCourseId(int courseId) {
    this.courseId = courseId;
  }

  public String getLocaleBcp47Tag() {
    return localeBcp47Tag;
  }

  public void setLocaleBcp47Tag(String localeBcp47Tag) {
    this.localeBcp47Tag = localeBcp47Tag;
  }

  public String getPictureUrl() {
    return pictureUrl;
  }

  public void setPictureUrl(String pictureUrl) {
    this.pictureUrl = pictureUrl;
  }

  public String getHeroPictureUrl() {
    return heroPictureUrl;
  }

  public void setHeroPictureUrl(String heroPictureUrl) {
    this.heroPictureUrl = heroPictureUrl;
  }

  public String getHeroPictureMobileUrl() {
    return heroPictureMobileUrl;
  }

  public void setHeroPictureMobileUrl(String heroPictureMobileUrl) {
    this.heroPictureMobileUrl = heroPictureMobileUrl;
  }
}
