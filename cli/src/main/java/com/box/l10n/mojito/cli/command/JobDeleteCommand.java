package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.apiclient.ScheduledJobClient;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * @author gerryyang
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"job-delete"},
    commandDescription = "Deletes a scheduled job")
public class JobDeleteCommand extends Command {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(JobDeleteCommand.class);

  @Parameter(
      names = {Param.JOB_UUID_LONG, Param.JOB_UUID_SHORT},
      arity = 1,
      required = true,
      description = Param.JOB_UUID_DESCRIPTION)
  String uuidParam;

  @Autowired ConsoleWriter consoleWriter;
  @Autowired
  ScheduledJobClient scheduledJobClient;

  @Override
  public void execute() throws CommandException {
    try {
      scheduledJobClient.deleteJob(UUID.fromString(uuidParam));
    } catch (Exception ex) {
      throw new CommandException(ex.getMessage(), ex);
    }
  }
}
