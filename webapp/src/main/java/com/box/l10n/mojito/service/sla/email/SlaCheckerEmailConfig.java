package com.box.l10n.mojito.service.sla.email;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** @author jeanaurambault */
@Configuration
@ConfigurationProperties(prefix = "l10n.sla-checker.email")
public class SlaCheckerEmailConfig {

  String from;

  String[] to;

  /**
   * Period to wait before re-sending an email during an incident.
   *
   * <p>The config property is a value in milliseconds.
   */
  Duration periodBetweenEmail = Duration.ofHours(1);

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String[] getTo() {
    return to;
  }

  public void setTo(String[] to) {
    this.to = to;
  }

  public Duration getPeriodBetweenEmail() {
    return periodBetweenEmail;
  }

  public void setPeriodBetweenEmail(Duration periodBetweenEmail) {
    this.periodBetweenEmail = periodBetweenEmail;
  }
}
