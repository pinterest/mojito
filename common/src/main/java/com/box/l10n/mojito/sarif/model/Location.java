package com.box.l10n.mojito.sarif.model;

public class Location {
  public PhysicalLocation physicalLocation;

  public Location(String uri, int startLine) {
    physicalLocation = new PhysicalLocation(uri, startLine);
  }

  public Location(String uri, int startLine, int endLine) {
    physicalLocation = new PhysicalLocation(uri, startLine, endLine);
  }

  public PhysicalLocation getPhysicalLocation() {
    return physicalLocation;
  }

  public void setPhysicalLocation(PhysicalLocation physicalLocation) {
    this.physicalLocation = physicalLocation;
  }
}
