package com.box.l10n.mojito.evolve;

import java.util.List;

public class CoursesDTO {

  private Pagination pagination;

  private List<CourseDTO> courses;

  public Pagination getPagination() {
    return pagination;
  }

  public void setPagination(Pagination pagination) {
    this.pagination = pagination;
  }

  public List<CourseDTO> getCourses() {
    return courses;
  }

  public void setCourses(List<CourseDTO> courses) {
    this.courses = courses;
  }
}
