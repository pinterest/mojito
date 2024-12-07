package com.box.l10n.mojito.cli.apiclient;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;

public class AuthenticatedApiInterceptor implements Interceptor {
  private static final String LOGIN_PATH = "/login";

  private static final String CSRF_TOKEN_HTML_NAME = "CSRF_TOKEN";

  private static final String CSRF_TOKEN_HEADER_NAME = "X-CSRF-TOKEN";

  private static final String COOKIE_NAME = "JSESSIONID";

  private String latestSessionId;

  private CsrfToken latestCsrfToken;

  private final CookieManager cookieManager;

  private final String basePath;

  public AuthenticatedApiInterceptor(CookieManager cookieManager, String basePath) {
    this.cookieManager = cookieManager;
    this.basePath = basePath;
  }

  private Optional<HttpCookie> getCookie() {
    CookieStore cookieStore = this.cookieManager.getCookieStore();
    return cookieStore.getCookies().stream()
        .filter(cookie -> cookie.getName().equalsIgnoreCase(COOKIE_NAME))
        .findFirst();
  }

  private CsrfToken getCsrfToken(OkHttpClient httpClient, Request request) throws IOException {
    Response response = httpClient.newCall(request).execute();
    try (ResponseBody body = response.body()) {
      Pattern pattern = Pattern.compile(CSRF_TOKEN_HTML_NAME + " = '(.*?)';");
      Matcher matcher = pattern.matcher(body.string());

      if (matcher.find()) {
        return new DefaultCsrfToken(CSRF_TOKEN_HEADER_NAME, "_csrf", matcher.group(1));
      } else {
        throw new SessionAuthenticationException(
            "Could not find " + CSRF_TOKEN_HTML_NAME + " variable on login page");
      }
    }
  }

  private void updateCsrfToken() throws IOException {
    HttpUrl.Builder urlBuilder = HttpUrl.parse(this.basePath + LOGIN_PATH).newBuilder();
    String url = urlBuilder.build().toString();
    OkHttpClient httpClient = new OkHttpClient();
    httpClient.setCookieHandler(CookieHandler.getDefault());
    Request request = new Request.Builder().url(url).build();
    this.latestCsrfToken = this.getCsrfToken(httpClient, request);
    RequestBody requestBody =
        new FormEncodingBuilder().add("username", "admin").add("password", "ChangeMe").build();
    request =
        new Request.Builder()
            .url(url)
            .addHeader(this.latestCsrfToken.getHeaderName(), this.latestCsrfToken.getToken())
            .post(requestBody)
            .build();
    this.latestCsrfToken = this.getCsrfToken(httpClient, request);
  }

  private synchronized void authenticate() throws IOException {
    Optional<HttpCookie> cookie = this.getCookie();
    if (cookie.isEmpty()
        || this.latestSessionId == null
        || !this.latestSessionId.equals(cookie.get().getValue())
        || this.latestCsrfToken == null) {
      try {
        this.updateCsrfToken();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    cookie = this.getCookie();
    if (cookie.isPresent()) {
      this.latestSessionId = cookie.get().getValue();
    } else {
      throw new SessionAuthenticationException("Could not find cookie: " + COOKIE_NAME);
    }
  }

  @Override
  public Response intercept(Chain chain) throws IOException {
    this.authenticate();
    final Request original = chain.request();
    final Request authenticated =
        original
            .newBuilder()
            .addHeader(this.latestCsrfToken.getHeaderName(), this.latestCsrfToken.getToken())
            .build();
    return chain.proceed(authenticated);
  }
}
