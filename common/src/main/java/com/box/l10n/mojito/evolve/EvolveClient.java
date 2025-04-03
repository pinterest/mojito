package com.box.l10n.mojito.evolve;

import com.box.l10n.mojito.iterators.ListWithLastPage;
import com.box.l10n.mojito.iterators.PageFetcherCurrentAndTotalPagesSplitIterator;
import com.google.common.collect.ImmutableMap;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

public class EvolveClient {
  private final String apiPath;

  private final RestTemplate restTemplate;

  private int maxRetries;

  private Duration retryMinBackoff;

  private Duration retryMaxBackoff;

  public EvolveClient(RestTemplate restTemplate, String apiPath) {
    this.restTemplate = restTemplate;
    this.apiPath = apiPath;
    this.maxRetries = 2;
    this.retryMinBackoff = Duration.ofSeconds(5);
    this.retryMaxBackoff = Duration.ofSeconds(30);
  }

  private String getFullEndpointPath(String endpointPath) {
    return this.apiPath + endpointPath;
  }

  private ListWithLastPage<CourseDTO> getCourses(String url) {
    CoursesDTO coursesDTO = this.restTemplate.getForObject(url, CoursesDTO.class);
    if (coursesDTO == null) {
      throw new RuntimeException("Empty response");
    }
    ListWithLastPage<CourseDTO> courseListWithLastPage = new ListWithLastPage<>();
    courseListWithLastPage.setList(coursesDTO.getCourses());
    courseListWithLastPage.setLastPage(coursesDTO.getPagination().getTotalPages());
    return courseListWithLastPage;
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
              return Mono.fromCallable(() -> this.getCourses(builderWithPage.toUriString()))
                  .retryWhen(
                      Retry.backoff(this.maxRetries, this.retryMinBackoff)
                          .maxBackoff(this.retryMaxBackoff))
                  .doOnError(
                      e -> {
                        throw new RuntimeException(e.getMessage(), e);
                      })
                  .block();
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
    return response;
  }

  public void updateCourse(
      int courseId, TranslationStatusType translationStatus, ZonedDateTime ifUnmodifiedSince) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setIfModifiedSince(ifUnmodifiedSince);
    Map<String, String> course = ImmutableMap.of("custom_j", translationStatus.getName());
    Map<String, Map<String, String>> courseBody = ImmutableMap.of("course", course);
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

  public void setMaxRetries(int maxRetries) {
    this.maxRetries = maxRetries;
  }

  public void setRetryMinBackoff(Duration retryMinBackoff) {
    this.retryMinBackoff = retryMinBackoff;
  }

  public void setRetryMaxBackoff(Duration retryMaxBackoff) {
    this.retryMaxBackoff = retryMaxBackoff;
  }
}
