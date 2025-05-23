package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.apiclient.UserClient;
import com.box.l10n.mojito.apiclient.exception.ResourceNotCreatedException;
import com.box.l10n.mojito.apiclient.model.Authority;
import com.box.l10n.mojito.apiclient.model.Role;
import com.box.l10n.mojito.apiclient.model.User;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.Console;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import java.util.List;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Command to add a user
 *
 * @author jyi
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"user-create"},
    commandDescription = "Creates a user")
public class UserCreateCommand extends Command {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(UserCreateCommand.class);

  @Autowired ConsoleWriter consoleWriter;

  @Autowired UserClient userClient;

  @Parameter(
      names = {Param.USERNAME_LONG, Param.USERNAME_SHORT},
      arity = 1,
      required = true,
      description = Param.USERNAME_DESCRIPTION)
  String username;

  @Parameter(
      names = {Param.ROLE_LONG, Param.ROLE_SHORT},
      arity = 1,
      required = false,
      description = Param.ROLE_DESCRIPTION)
  String rolename;

  @Parameter(
      names = {Param.SURNAME_LONG, Param.SURNAME_SHORT},
      arity = 1,
      required = true,
      description = Param.SURNAME_DESCRIPTION)
  String surname;

  @Parameter(
      names = {Param.GIVEN_NAME_LONG, Param.GIVEN_NAME_SHORT},
      arity = 1,
      required = true,
      description = Param.GIVEN_NAME_DESCRIPTION)
  String givenName;

  @Parameter(
      names = {Param.COMMON_NAME_LONG, Param.COMMON_NAME_SHORT},
      arity = 1,
      required = true,
      description = Param.COMMON_NAME_DESCRIPTION)
  String commonName;

  @Autowired Console console;

  @Override
  protected void execute() throws CommandException {
    consoleWriter.a("Create user: ").fg(Ansi.Color.CYAN).a(username).println();

    try {
      consoleWriter.a("Enter new password for " + username + ":").println();
      String password = console.readPassword();

      User userBody = new User();
      userBody.setUsername(username);
      userBody.setPassword(password);
      userBody.setSurname(surname);
      userBody.setGivenName(givenName);
      userBody.setCommonName(commonName);

      Role role = Role.fromString(this.rolename);
      if (role != null) {
        Authority authority = new Authority();
        authority.setAuthority(role.toString());
        userBody.setAuthorities(List.of(authority));
      }
      User user = userClient.createUser(userBody);
      consoleWriter
          .newLine()
          .a("created --> user: ")
          .fg(Ansi.Color.MAGENTA)
          .a(user.getUsername())
          .println();
    } catch (ResourceNotCreatedException ex) {
      throw new CommandException(ex.getMessage(), ex);
    }
  }
}
