package com.box.l10n.mojito.evolve;

import com.box.l10n.mojito.iterators.ListWithLastPage;
import com.box.l10n.mojito.iterators.PageFetcherCurrentAndTotalPagesSplitIterator;
import com.box.l10n.mojito.json.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class EvolveClient {
  static Logger logger = LoggerFactory.getLogger(EvolveClient.class);

  private final String apiPath;

  private final RestTemplate restTemplate;

  public EvolveClient(RestTemplate restTemplate, String apiPath) {
    this.restTemplate = restTemplate;
    this.apiPath = apiPath;
  }

  private String getFullEndpointPath(String endpointPath) {
    return this.apiPath + endpointPath;
  }

  public Stream<CourseDTO> getCourses() {

    PageFetcherCurrentAndTotalPagesSplitIterator<CourseDTO> iterator =
        new PageFetcherCurrentAndTotalPagesSplitIterator<>(
            pageToFetch -> {
              CoursesDTO coursesDTO =
                  restTemplate.getForObject(
                      "courses?currentPage={currentPage}", CoursesDTO.class, pageToFetch);
              ListWithLastPage<CourseDTO> courseListWithLastPage = new ListWithLastPage<>();
              courseListWithLastPage.setList(coursesDTO.getCourses());
              courseListWithLastPage.setLastPage(coursesDTO.getPagination().getTotalPages());
              return courseListWithLastPage;
            },
            1);

    return StreamSupport.stream(iterator, false);
  }

  /**
   * Gets the subset of the course needed for translation
   *
   * @param courseId
   * @return
   */
  public String getTranslationsByCourseId(int courseId) {
    return restTemplate.getForObject("translate/{courseId}", String.class, courseId);
  }

  /**
   * Create a translated course.
   *
   * @param courseId
   * @param translatedCourse
   * @param locale
   * @param isRTL
   */
  public void createCourseTranslationsById(
      int courseId, String translatedCourse, String locale, boolean isRTL) {
    translatedCourse = addVersionAndRtlAttributeToTranslations(translatedCourse, locale, isRTL);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> httpEntity = new HttpEntity<>(translatedCourse, headers);
    String response =
        this.restTemplate.postForObject("translate/{courseId}", httpEntity, String.class, courseId);
    logger.debug("course created: {}", response);
  }

  String addVersionAndRtlAttributeToTranslations(
      String translatedCourse, String locale, boolean isRTL) {
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode jsonNode = objectMapper.readTreeUnchecked(translatedCourse);
    JsonNode translations = jsonNode.get("_translations");
    if (jsonNode == null || !jsonNode.isObject()) {
      throw new RuntimeException("there must be a _translations object node");
    }
    ObjectNode translationsObjectNode = (ObjectNode) translations;
    translationsObjectNode.put("_version", locale);
    translationsObjectNode.put("_rtl", isRTL);
    return objectMapper.writeValueAsStringUnchecked(jsonNode);
  }

  public Stream<CourseDTO> getCourses(CoursesGetRequest request) {
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromPath(this.getFullEndpointPath("courses"))
            .queryParam("locale", request.locale())
            .queryParam("is_active", request.active());
    if (request.updatedOnFrom() != null) {
      builder.queryParam("updated_on_from", request.updatedOnFrom());
    }
    if (request.updatedOnTo() != null) {
      builder.queryParam("updated_on_to", request.updatedOnTo());
    }
    PageFetcherCurrentAndTotalPagesSplitIterator<CourseDTO> iterator =
        new PageFetcherCurrentAndTotalPagesSplitIterator<>(
            pageToFetch -> {
              UriComponentsBuilder builderWithPage =
                  builder.cloneBuilder().queryParam("page", pageToFetch);
              CoursesDTO coursesDTO =
                  this.restTemplate.getForObject(builderWithPage.toUriString(), CoursesDTO.class);
              if (coursesDTO == null) {
                throw new RuntimeException("Empty response");
              }
              ListWithLastPage<CourseDTO> courseListWithLastPage = new ListWithLastPage<>();
              courseListWithLastPage.setList(coursesDTO.getCourses());
              courseListWithLastPage.setLastPage(coursesDTO.getPagination().getTotalPages());
              return courseListWithLastPage;
            },
            1);

    return StreamSupport.stream(iterator, false);
  }

  public String startCourseTranslation(
      int courseId, String targetLocale, Set<String> additionalLocales) {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.APPLICATION_XML));
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromPath(this.getFullEndpointPath("course_translations/{courseId}"))
            .queryParam("target_locale", targetLocale);
    if (additionalLocales != null && !additionalLocales.isEmpty()) {
      builder.queryParam("additional_locales[]", additionalLocales);
    }

    HttpEntity<String> httpEntity = new HttpEntity<>(headers);
    String response =
        this.restTemplate.postForObject(
            builder.buildAndExpand(courseId).toUriString(), httpEntity, String.class);
    logger.debug("course created: {}", response);
    return response;
  }

  public void updateCourse(
      int courseId, TranslationStatusType translationStatus, ZonedDateTime ifUnmodifiedSince) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setIfModifiedSince(ifUnmodifiedSince);
    Map<String, String> course = new HashMap<>();
    course.put("custom_j", translationStatus.getName());
    Map<String, Map<String, String>> courseBody = new HashMap<>();
    courseBody.put("course", course);
    this.restTemplate.put(
        this.getFullEndpointPath("courses/{courseId}"),
        new HttpEntity<>(courseBody, headers),
        courseId);
  }

  public void updateCourseTranslation(int courseId, String translatedCourse) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_XML);
    this.restTemplate.put(
        this.getFullEndpointPath("course_translations/{courseId}"),
        new HttpEntity<>(translatedCourse, headers),
        courseId);
  }
}
