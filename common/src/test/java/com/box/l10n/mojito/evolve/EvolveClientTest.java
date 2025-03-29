package com.box.l10n.mojito.evolve;

import static com.box.l10n.mojito.evolve.TranslationStatusType.IN_TRANSLATION;
import static com.box.l10n.mojito.evolve.TranslationStatusType.READY_FOR_TRANSLATION;
import static com.box.l10n.mojito.evolve.TranslationStatusType.TRANSLATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {EvolveClientTest.class})
public class EvolveClientTest {
  final String apiPath = "api/v3/";

  @Mock RestTemplate mockRestTemplate;

  EvolveClient evolveClient;

  @Captor ArgumentCaptor<String> urlCaptor;

  @Captor ArgumentCaptor<HttpEntity<Object>> httpEntityCaptor;

  @Captor ArgumentCaptor<Integer> courseIdCaptor;

  private void initData() {
    CourseDTO courseDTO1 = new CourseDTO();
    courseDTO1.setId(1);
    courseDTO1.setUpdatedOn(ZonedDateTime.now());
    courseDTO1.setTranslationStatus(READY_FOR_TRANSLATION);

    CourseDTO courseDTO2 = new CourseDTO();
    courseDTO2.setId(2);
    courseDTO2.setUpdatedOn(ZonedDateTime.now());
    courseDTO2.setTranslationStatus(IN_TRANSLATION);

    CourseDTO courseDTO3 = new CourseDTO();
    courseDTO3.setId(3);
    courseDTO3.setUpdatedOn(ZonedDateTime.now());
    courseDTO3.setTranslationStatus(TRANSLATED);

    CourseDTO courseDTO4 = new CourseDTO();
    courseDTO4.setId(4);
    courseDTO4.setUpdatedOn(ZonedDateTime.now());
    courseDTO4.setTranslationStatus(null);

    CoursesDTO coursesDTO1 = new CoursesDTO();
    coursesDTO1.setCourses(List.of(courseDTO1, courseDTO2, courseDTO3));
    Pagination pagination1 = new Pagination();
    pagination1.setCurrentPage(1);
    pagination1.setTotalPages(2);
    coursesDTO1.setPagination(pagination1);

    CoursesDTO coursesDTO2 = new CoursesDTO();
    coursesDTO2.setCourses(List.of(courseDTO4));
    Pagination pagination2 = new Pagination();
    pagination2.setCurrentPage(2);
    pagination2.setTotalPages(2);
    coursesDTO2.setPagination(pagination2);
    when(this.mockRestTemplate.getForObject(anyString(), any()))
        .thenReturn(coursesDTO1)
        .thenReturn(coursesDTO2);
    this.evolveClient = new EvolveClient(this.mockRestTemplate, this.apiPath);
  }

  @Test
  public void testGetCourses() {
    this.initData();
    CoursesGetRequest coursesGetRequest = new CoursesGetRequest("en", null, null);

    List<CourseDTO> courses = this.evolveClient.getCourses(coursesGetRequest).toList();

    verify(this.mockRestTemplate, times(2)).getForObject(urlCaptor.capture(), any());

    assertEquals(urlCaptor.getAllValues().size(), 2);
    assertEquals(
        this.apiPath + "courses?locale=en&is_active=true&page=1",
        urlCaptor.getAllValues().getFirst());
    assertEquals(
        this.apiPath + "courses?locale=en&is_active=true&page=2",
        urlCaptor.getAllValues().getLast());

    for (CourseDTO courseDTO : courses) {
      if (courseDTO.getTranslationStatus() == null) {
        assertEquals(4, courseDTO.getId());
      } else if (courseDTO.getTranslationStatus() == READY_FOR_TRANSLATION) {
        assertEquals(1, courseDTO.getId());
      } else if (courseDTO.getTranslationStatus() == IN_TRANSLATION) {
        assertEquals(2, courseDTO.getId());
      } else if (courseDTO.getTranslationStatus() == TRANSLATED) {
        assertEquals(3, courseDTO.getId());
      }
    }
  }

  private void initEmptyData() {
    reset(this.mockRestTemplate);
    CoursesDTO coursesDTO = new CoursesDTO();
    coursesDTO.setCourses(List.of());
    Pagination pagination1 = new Pagination();
    pagination1.setCurrentPage(1);
    pagination1.setTotalPages(1);
    coursesDTO.setPagination(pagination1);
    when(this.mockRestTemplate.getForObject(anyString(), any())).thenReturn(coursesDTO);
    this.evolveClient = new EvolveClient(this.mockRestTemplate, this.apiPath);
  }

  @Test
  public void testGetCoursesWithUpdatedOnToAndUpdatedOnFrom() {
    this.initEmptyData();

    ZonedDateTime updatedOnTo = ZonedDateTime.now();
    ZonedDateTime updatedOnFrom = updatedOnTo.minusDays(1);
    CoursesGetRequest coursesGetRequest = new CoursesGetRequest("en", updatedOnFrom, null);
    long count = this.evolveClient.getCourses(coursesGetRequest).count();

    assertEquals(0, count);

    verify(this.mockRestTemplate, times(1)).getForObject(urlCaptor.capture(), any());

    assertEquals(
        this.apiPath
            + String.format(
                "courses?locale=en&is_active=true&updated_on_from=%s&page=1",
                UriComponentsBuilder.fromPath(updatedOnFrom.toString()).toUriString()),
        urlCaptor.getValue());

    this.initEmptyData();

    coursesGetRequest = new CoursesGetRequest("en", null, updatedOnTo);
    count = this.evolveClient.getCourses(coursesGetRequest).count();

    assertEquals(0, count);

    verify(this.mockRestTemplate, times(1)).getForObject(urlCaptor.capture(), any());

    assertEquals(
        this.apiPath
            + String.format(
                "courses?locale=en&is_active=true&updated_on_to=%s&page=1",
                UriComponentsBuilder.fromPath(updatedOnTo.toString()).toUriString()),
        urlCaptor.getValue());

    this.initEmptyData();

    coursesGetRequest = new CoursesGetRequest("en", updatedOnFrom, updatedOnTo);
    count = this.evolveClient.getCourses(coursesGetRequest).count();

    assertEquals(0, count);

    verify(this.mockRestTemplate, times(1)).getForObject(urlCaptor.capture(), any());

    assertEquals(
        this.apiPath
            + String.format(
                "courses?locale=en&is_active=true&updated_on_from=%s&updated_on_to=%s&page=1",
                UriComponentsBuilder.fromPath(updatedOnFrom.toString()).toUriString(),
                UriComponentsBuilder.fromPath(updatedOnTo.toString()).toUriString()),
        urlCaptor.getValue());
  }

  @Test
  public void testGetCoursesWithZeroPages() {
    CoursesDTO coursesDTO = new CoursesDTO();
    coursesDTO.setCourses(List.of());
    Pagination pagination1 = new Pagination();
    pagination1.setCurrentPage(1);
    pagination1.setTotalPages(0);
    coursesDTO.setPagination(pagination1);
    when(this.mockRestTemplate.getForObject(anyString(), any())).thenReturn(coursesDTO);
    this.evolveClient = new EvolveClient(this.mockRestTemplate, this.apiPath);

    CoursesGetRequest coursesGetRequest = new CoursesGetRequest("en", null, null);

    long count = this.evolveClient.getCourses(coursesGetRequest).count();

    assertEquals(0, count);
  }

  private void initDataForCourseTranslation() {
    Mockito.reset(this.mockRestTemplate);
    when(this.mockRestTemplate.postForObject(anyString(), any(), any())).thenReturn("content");
    this.evolveClient = new EvolveClient(this.mockRestTemplate, this.apiPath);
  }

  @Test
  public void testStartCourseTranslation() {
    this.initDataForCourseTranslation();

    String response = this.evolveClient.startCourseTranslation(1, "es", Sets.newHashSet());

    verify(this.mockRestTemplate, times(1))
        .postForObject(this.urlCaptor.capture(), this.httpEntityCaptor.capture(), any());

    assertEquals(this.apiPath + "course_translations/1?target_locale=es", urlCaptor.getValue());
    HttpEntity<Object> httpEntity = this.httpEntityCaptor.getValue();
    assertTrue(httpEntity.getHeaders().getAccept().contains(MediaType.APPLICATION_XML));
    assertEquals("content", response);

    this.initDataForCourseTranslation();

    this.evolveClient.startCourseTranslation(1, "es", Sets.newHashSet("fr", "it"));

    verify(this.mockRestTemplate, times(1))
        .postForObject(this.urlCaptor.capture(), any(HttpEntity.class), any());

    assertEquals(
        this.apiPath
            + "course_translations/1?target_locale=es&additional_locales[]=fr&additional_locales[]=it",
        urlCaptor.getValue());
  }

  @Test
  public void testUpdateCourse() {
    this.evolveClient = new EvolveClient(this.mockRestTemplate, this.apiPath);

    ZonedDateTime modifiedSince = ZonedDateTime.now();
    this.evolveClient.updateCourse(1, IN_TRANSLATION, modifiedSince);

    verify(this.mockRestTemplate)
        .put(
            this.urlCaptor.capture(),
            this.httpEntityCaptor.capture(),
            this.courseIdCaptor.capture());

    assertEquals(this.apiPath + "courses/{courseId}", this.urlCaptor.getValue());
    assertEquals(1, (int) courseIdCaptor.getValue());
    HttpEntity<Object> httpEntity = this.httpEntityCaptor.getValue();
    HttpHeaders headers = httpEntity.getHeaders();
    assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
    assertEquals(modifiedSince.toEpochSecond() * 1000, headers.getIfModifiedSince());
    assertTrue(httpEntity.getBody() instanceof Map);
    Map<?, ?> body = (Map<?, ?>) httpEntity.getBody();
    assertTrue(body.get("course") instanceof Map);
    assertEquals(((Map<?, ?>) body.get("course")).get("custom_j"), IN_TRANSLATION.getName());
  }

  @Test
  public void testUpdateCourseTranslation() {
    this.evolveClient = new EvolveClient(this.mockRestTemplate, this.apiPath);

    this.evolveClient.updateCourseTranslation(1, "content");

    verify(this.mockRestTemplate, times(1))
        .put(
            this.urlCaptor.capture(),
            this.httpEntityCaptor.capture(),
            this.courseIdCaptor.capture());

    assertEquals(this.apiPath + "course_translations/{courseId}", this.urlCaptor.getValue());
    HttpEntity<Object> httpEntity = this.httpEntityCaptor.getValue();
    assertEquals(MediaType.APPLICATION_XML, httpEntity.getHeaders().getContentType());
    assertEquals("content", httpEntity.getBody());
    assertEquals(1, (int) courseIdCaptor.getValue());
  }
}
