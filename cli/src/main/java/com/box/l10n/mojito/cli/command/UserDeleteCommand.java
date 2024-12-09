package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.apiclient.ApiClient;
import com.box.l10n.mojito.cli.apiclient.ApiException;
import com.box.l10n.mojito.cli.apiclient.UserWsApiProxy;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import javax.annotation.PostConstruct;
import org.fusesource.jansi.Ansi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Command to delete a user
 *
 * @author jyi
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"user-delete"},
    commandDescription = "Deletes a user")
public class UserDeleteCommand extends Command {

  @Parameter(
      names = {Param.USERNAME_LONG, Param.USERNAME_SHORT},
      arity = 1,
      required = true,
      description = Param.USERNAME_DESCRIPTION)
  String username;

  @Autowired ConsoleWriter consoleWriter;

  @Autowired ApiClient apiClient;

  private UserWsApiProxy userClient;

  @PostConstruct
  protected void init() {
    this.userClient = new UserWsApiProxy(this.apiClient);
  }

  @Override
  protected void execute() throws CommandException {
    consoleWriter.a("Delete user: ").fg(Ansi.Color.CYAN).a(username).println();

    try {
      this.userClient.deleteUserByUsername(username);
      consoleWriter.newLine().a("deleted --> user: ").fg(Ansi.Color.MAGENTA).a(username).println();
    } catch (ApiException ex) {
      throw new CommandException(ex.getMessage(), ex);
    }
  }
}
