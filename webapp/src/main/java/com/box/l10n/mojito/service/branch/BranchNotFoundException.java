package com.box.l10n.mojito.service.branch;

public class BranchNotFoundException extends Throwable {
  public BranchNotFoundException(Exception e) {
    super(e);
  }

  public BranchNotFoundException(String message) {
    super(message);
  }
}
