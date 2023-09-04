package com.box.l10n.mojito.rest.resttemplate;

import com.google.common.base.Strings;
import java.util.List;
import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.stereotype.Component;

@Component
public class CsrfTokenService {

  Logger logger = LoggerFactory.getLogger(CsrfTokenService.class);

  /**
   * This is the lastest CSRF token that was obtained from the {@link
   * this#latestSessionIdForLatestCsrfToken}
   */
  String latestSessionIdForLatestCsrfToken;

  /** This is the latest session id that was used to obtain the {@link this#latestCsrfToken} */
  CsrfToken latestCsrfToken = null;

  /**
   * This is so that we obtain access to the cookie store used inside HttpClient to check to see if
   * we have a session
   */
  CookieStore cookieStore;

  public static final String CSRF_PARAM_NAME = "_csrf";
  public static final String CSRF_HEADER_NAME = "X-CSRF-TOKEN";
  public static final String COOKIE_SESSION_NAME = "JSESSIONID";

  @Autowired ResttemplateConfig resttemplateConfig;

  /**
   * This is used for the authentication flow to keep things separate from the restTemplate that
   * this interceptor is intercepting
   */
  CookieStoreRestTemplate restTemplateForAuthenticationFlow;

  /**
   * Use the CSRF token endpoint to get the CSRF token corresponding to this session
   *
   * @param csrfTokenUrl The full URL to which the CSRF token can be obtained
   * @return
   */
  protected CsrfToken getCsrfTokenFromEndpoint(String csrfTokenUrl) {
    ResponseEntity<String> csrfTokenEntity =
        restTemplateForAuthenticationFlow.getForEntity(csrfTokenUrl, String.class, "");
    logger.debug("CSRF token from {} is {}", csrfTokenUrl, csrfTokenEntity.getBody());
    return new DefaultCsrfToken(CSRF_HEADER_NAME, CSRF_PARAM_NAME, csrfTokenEntity.getBody());
  }

  /**
   * Gets the authenticated session id. If it is not found, an authentication flow will be started
   * so that a proper session id is available
   *
   * @return
   */
  protected boolean doesSessionIdInCookieStoreExistAndMatchLatestSessionId() {
    logger.debug(
        "Check to see if session id in cookie store matches the session id used to get the latest CSRF token");
    String sessionId = getAuthenticationSessionIdFromCookieStore();

    return sessionId != null && sessionId.equals(latestSessionIdForLatestCsrfToken);
  }

  /** @return null if no sesson id cookie is found */
  protected String getAuthenticationSessionIdFromCookieStore() {
    List<Cookie> cookies = cookieStore.getCookies();
    for (Cookie cookie : cookies) {
      if (cookie.getName().equals(COOKIE_SESSION_NAME)) {
        String cookieValue = cookie.getValue();
        logger.debug("Found session cookie: {}", cookieValue);
        return cookieValue;
      }
    }

    return null;
  }

  /**
   * Gets the full URI (scheme + host + port + path) to access the REST WS for a given resource
   * path.
   *
   * <p>If the resource path starts with the http scheme it is considered already as a full URI and
   * is returned as it.
   *
   * @param resourcePath the resource path (possibly a full URI)
   * @return full URI of the REST WS
   */
  public String getURIForResource(String resourcePath) {

    StringBuilder uri = new StringBuilder();

    if (resourcePath.startsWith(resttemplateConfig.getScheme())) {
      uri.append(resourcePath);
    } else {
      uri.append(resttemplateConfig.getScheme()).append("://").append(resttemplateConfig.getHost());

      if (resttemplateConfig.getPort() != 80) {
        uri.append(":").append(resttemplateConfig.getPort());
      }

      if (!Strings.isNullOrEmpty(resttemplateConfig.getContextPath())) {
        uri.append(resttemplateConfig.getContextPath());
      }

      uri.append("/").append(resourcePath);
    }

    return uri.toString();
  }
}
