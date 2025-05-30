package com.box.l10n.mojito.rest.ai;

import java.net.http.HttpResponse;

public class AIException extends RuntimeException {
  HttpResponse httpResponse;

  public AIException(String message) {
    super(message);
  }

  public AIException(String message, Exception e) {
    super(message, e);
  }

  public AIException(String message, Throwable t) {
    super(message, t);
  }

  public AIException(String message, Exception e, HttpResponse httpResponse) {
    super(message, e);
    this.httpResponse = httpResponse;
  }

  public HttpResponse getHttpResponse() {
    return httpResponse;
  }
}
