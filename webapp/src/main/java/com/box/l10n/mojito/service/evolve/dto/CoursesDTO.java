package com.box.l10n.mojito.service.evolve.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CoursesDTO {

  private PaginationDTO pagination;

  private List<CourseDTO> courses;

  public PaginationDTO getPagination() {
    return pagination;
  }

  public void setPagination(PaginationDTO pagination) {
    this.pagination = pagination;
  }

  public List<CourseDTO> getCourses() {
    return courses;
  }

  public void setCourses(List<CourseDTO> courses) {
    this.courses = courses;
  }
}
