package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.apiclient.ApiException;
import com.box.l10n.mojito.cli.apiclient.UserWsApi;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.cli.model.Authority;
import com.box.l10n.mojito.cli.model.PageUser;
import com.box.l10n.mojito.cli.model.Pageable;
import com.box.l10n.mojito.cli.model.User;
import com.box.l10n.mojito.rest.entity.Role;
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
    PageUser pageUser = this.userClient.getUsers(pageable, username, null);
    if (pageUser.getContent().isEmpty()) {
      throw new CommandException("User with username [" + username + "] is not found");
    }
    return pageUser;
  }

  protected void addAuthorities(User user, String roleName) {
    Role role = Role.fromString(roleName);
    if (role != null) {
      Authority authority = new Authority();
      authority.setAuthority(role.toString());
      user.setAuthorities(List.of(authority));
    }
  }
}
