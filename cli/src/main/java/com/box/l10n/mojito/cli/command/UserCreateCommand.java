package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.apiclient.ApiException;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.Console;
import com.box.l10n.mojito.cli.model.User;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
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
public class UserCreateCommand extends UserCommand {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(UserCreateCommand.class);

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
      this.addAuthorities(userBody, this.rolename);

      User user = userClient.createUser(userBody);
      consoleWriter
          .newLine()
          .a("created --> user: ")
          .fg(Ansi.Color.MAGENTA)
          .a(user.getUsername())
          .println();
    } catch (ApiException ex) {
      if (ex.getCode() == HttpStatus.CONFLICT.value()) {
        throw new CommandException("User with username [" + username + "] already exists");
      }
      throw new CommandException(ex.getMessage(), ex);
    }
  }
}
