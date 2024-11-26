package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.apiclient.ApiException;
import com.box.l10n.mojito.cli.apiclient.UserWsApi;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.cli.model.PageUser;
import com.box.l10n.mojito.cli.model.Pageable;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class UserCommand extends Command {
  @Autowired UserWsApi userClient;

  @Autowired ConsoleWriter consoleWriter;

  protected PageUser getPageUser(String username) throws ApiException {
    Pageable pageable = new Pageable();
    pageable.setPage(0);
    pageable.setSize(Integer.MAX_VALUE);
    pageable.setSort(List.of());
    return this.userClient.getUsers(pageable, username, null);
  }
}
