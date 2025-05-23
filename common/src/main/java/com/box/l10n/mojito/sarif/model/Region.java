package com.box.l10n.mojito.sarif.model;

import java.util.Optional;

public class Region {
  public int startLine;

  public Optional<Integer> endLine;

  public Region(int startLine) {
    this.startLine = startLine;
    this.endLine = Optional.empty();
  }

  public Region(int startLine, int endLine) {
    this.startLine = startLine;
    this.endLine = Optional.of(endLine);
  }

  public int getStartLine() {
    return startLine;
  }

  public void setStartLine(int startLine) {
    this.startLine = startLine;
  }

  public Optional<Integer> getEndLine() {
    return endLine;
  }

  public void setEndLine(Optional<Integer> endLine) {
    this.endLine = endLine;
  }
}
