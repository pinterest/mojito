package com.box.l10n.mojito.sarif.model;

import java.util.ArrayList;
import java.util.List;

public class Result {
  private String ruleId;
  private ResultLevel level;
  private Message message;
  private List<Location> locations = new ArrayList<>();

  public Result(String ruleId, String text, ResultLevel level) {
    this.ruleId = ruleId;
    this.message = new Message(text);
    this.level = level;
  }

  public List<Location> getLocations() {
    return locations;
  }

  public void setLocations(List<Location> locations) {
    this.locations = locations;
  }

  public String getRuleId() {
    return ruleId;
  }

  public void setRuleId(String ruleId) {
    this.ruleId = ruleId;
  }

  public ResultLevel getLevel() {
    return level;
  }

  public void setLevel(ResultLevel level) {
    this.level = level;
  }

  public Message getMessage() {
    return message;
  }

  public void setMessage(Message message) {
    this.message = message;
  }

  public void addLocation(Location location) {
    this.locations.add(location);
  }
}
