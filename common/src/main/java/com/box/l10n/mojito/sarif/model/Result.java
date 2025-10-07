package com.box.l10n.mojito.sarif.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

public class Result {
  private String ruleId;
  private ResultLevel level;
  private Message message;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private List<Location> locations = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private Map<String, String> properties;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private Map<String, String> partialFingerprints;

  public Result(
      String ruleId,
      String text,
      String markdown,
      ResultLevel level,
      Map<String, String> properties) {
    this.ruleId = ruleId;
    this.message = new Message(text, markdown);
    this.level = level;
    this.properties = properties;
  }

  public Result(String ruleId, String text, String markdown, ResultLevel level) {
    this.ruleId = ruleId;
    this.message = new Message(text, markdown);
    this.level = level;
  }

  public List<Location> getLocations() {
    return locations;
  }

  public void setLocations(List<Location> locations) {
    this.locations = locations;
    try {
      StringBuilder sb = new StringBuilder(this.ruleId);
      sb.append(":\n");
      locations.forEach(
          location -> {
            String uri = location.getPhysicalLocation().getArtifactLocation().getUri();
            Region region = location.getPhysicalLocation().getRegion();
            sb.append(uri);
            sb.append(':');
            sb.append(region.startLine);
            sb.append('&');
            sb.append(region.endLine);
            sb.append('\n');
          });
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
      String primaryLocationLineHash = HexFormat.of().formatHex(hash);
      this.partialFingerprints = Map.of("primaryLocationLineHash", primaryLocationLineHash);
    } catch (NoSuchAlgorithmException ignored) {

    }
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

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  public Map<String, String> getPartialFingerprints() {
    return this.partialFingerprints;
  }
}
