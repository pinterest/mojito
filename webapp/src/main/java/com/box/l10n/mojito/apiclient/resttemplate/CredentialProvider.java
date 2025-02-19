package com.box.l10n.mojito.apiclient.resttemplate;

import org.springframework.stereotype.Component;

@Component
public interface CredentialProvider {

  String getUsername();

  String getPassword();
}
