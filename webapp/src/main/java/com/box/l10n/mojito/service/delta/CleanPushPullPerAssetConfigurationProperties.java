package com.box.l10n.mojito.service.delta;

import com.google.common.base.Preconditions;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n.push-pull-run.cleanup-job.cleanup-per-asset")
public class CleanPushPullPerAssetConfigurationProperties {
  private boolean enabled = true;

  private int startDayOfFirstRange = 15;

  private int endDayOfFirstRange = 21;

  private int startDayOfSecondRange = 22;

  private int endDayOfSecondRange = 31;

  @PostConstruct
  public void init() {
    Preconditions.checkArgument(this.startDayOfFirstRange <= this.endDayOfFirstRange);
    Preconditions.checkArgument(this.startDayOfSecondRange <= this.endDayOfSecondRange);
    Preconditions.checkArgument(this.endDayOfFirstRange <= this.startDayOfSecondRange);
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

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
