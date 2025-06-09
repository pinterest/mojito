package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author wyau
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"job-create"},
    commandDescription = "Creates a scheduled job")
public class JobCreateCommand extends Command {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(JobCreateCommand.class);

  @Parameter(
      names = {Param.REPOSITORY_NAME_LONG, Param.REPOSITORY_NAME_SHORT},
      arity = 1,
      required = true,
      description = Param.REPOSITORY_NAME_DESCRIPTION)
  String nameParam;

  @Override
  public void execute() throws CommandException {
    try {
        logger.info(nameParam);
    } catch (Exception ex) {
      throw new CommandException(ex.getMessage(), ex);
    }
  }
}
