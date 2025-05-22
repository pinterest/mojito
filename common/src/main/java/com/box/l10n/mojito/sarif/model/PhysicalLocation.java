package com.box.l10n.mojito.sarif.model;

public class PhysicalLocation {
  public ArtifactLocation artifactLocation;
  public Region region;

  PhysicalLocation(String uri, int startLine) {
    artifactLocation = new ArtifactLocation(uri);
    region = new Region(startLine);
  }

  PhysicalLocation(String uri, int startLine, int endLine) {
    artifactLocation = new ArtifactLocation(uri);
    region = new Region(startLine, endLine);
  }

  public Region getRegion() {
    return region;
  }

  public void setRegion(Region region) {
    this.region = region;
  }

  public ArtifactLocation getArtifactLocation() {
    return artifactLocation;
  }

  public void setArtifactLocation(ArtifactLocation artifactLocation) {
    this.artifactLocation = artifactLocation;
  }
}
