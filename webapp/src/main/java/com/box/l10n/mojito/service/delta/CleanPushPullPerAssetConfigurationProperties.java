package com.box.l10n.mojito.service.delta;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n.push-pull-run.cleanup-job.cleanup-per-asset")
public class CleanPushPullPerAssetConfigurationProperties {
  private int startDayOfFirstRange = 15;

  private int endDayOfFirstRange = 21;

  private int startDayOfSecondRange = 22;

  private int endDayOfSecondRange = 31;

  public int getStartDayOfFirstRange() {
    return startDayOfFirstRange;
  }

  public void setStartDayOfFirstRange(int startDayOfFirstRange) {
    this.startDayOfFirstRange = startDayOfFirstRange;
  }

  public int getEndDayOfFirstRange() {
    return endDayOfFirstRange;
  }

  public void setEndDayOfFirstRange(int endDayOfFirstRange) {
    this.endDayOfFirstRange = endDayOfFirstRange;
  }

  public int getStartDayOfSecondRange() {
    return startDayOfSecondRange;
  }

  public void setStartDayOfSecondRange(int startDayOfSecondRange) {
    this.startDayOfSecondRange = startDayOfSecondRange;
  }

  public int getEndDayOfSecondRange() {
    return endDayOfSecondRange;
  }

  public void setEndDayOfSecondRange(int endDayOfSecondRange) {
    this.endDayOfSecondRange = endDayOfSecondRange;
  }
}
