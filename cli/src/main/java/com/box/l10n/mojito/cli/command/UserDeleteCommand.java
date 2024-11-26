package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.rest.client.exception.ResourceNotFoundException;
import io.swagger.client.ApiException;
import io.swagger.client.model.User;
import java.util.List;
import org.fusesource.jansi.Ansi;
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
public class UserDeleteCommand extends UserCommand {

  @Parameter(
      names = {Param.USERNAME_LONG, Param.USERNAME_SHORT},
      arity = 1,
      required = true,
      description = Param.USERNAME_DESCRIPTION)
  String username;

  @Override
  protected void execute() throws CommandException {
    consoleWriter.a("Delete user: ").fg(Ansi.Color.CYAN).a(username).println();

    try {
      List<User> users = this.getPageUser(username).getContent();
      if (users.isEmpty()) {
        throw new ResourceNotFoundException("User with username [" + username + "] is not found");
      }
      this.userClient.deleteUserByUserId(users.getFirst().getId());
      consoleWriter.newLine().a("deleted --> user: ").fg(Ansi.Color.MAGENTA).a(username).println();
    } catch (ResourceNotFoundException | ApiException ex) {
      throw new CommandException(ex.getMessage(), ex);
    }
  }
}
