package com.box.l10n.mojito.cli.resttemplate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SystemPromptCredentialProvider implements CredentialProvider {

  @Value("${user.name}")
  String systemUserName;

  String password;

  @Override
  public String getUsername() {
    return systemUserName;
  }

  @Override
  public String getPassword() {
    if (password == null) {
      System.out.println("Enter password for mojito user " + systemUserName + ": ");
      char[] readPassword = System.console().readPassword();
      password = new String(readPassword);
    }

    return password;
  }
}
