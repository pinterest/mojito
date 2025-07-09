package com.box.l10n.mojito.service.delta;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n.push-pull-run.cleanup-job.cleanup-per-asset")
public class CleanPushPullPerAssetConfigurationProperties {
  private int startDayFirstRange = 15;

  private int endDayFirstRange = 21;

  private int startDaySecondRange = 22;

  private int endDaySecondRange = 31;

  public int getStartDayFirstRange() {
    return startDayFirstRange;
  }

  public void setStartDayFirstRange(int startDayFirstRange) {
    this.startDayFirstRange = startDayFirstRange;
  }

  public int getEndDayFirstRange() {
    return endDayFirstRange;
  }

  public void setEndDayFirstRange(int endDayFirstRange) {
    this.endDayFirstRange = endDayFirstRange;
  }

  public int getStartDaySecondRange() {
    return startDaySecondRange;
  }

  public void setStartDaySecondRange(int startDaySecondRange) {
    this.startDaySecondRange = startDaySecondRange;
  }

  public int getEndDaySecondRange() {
    return endDaySecondRange;
  }

  public void setEndDaySecondRange(int endDaySecondRange) {
    this.endDaySecondRange = endDaySecondRange;
  }
}
