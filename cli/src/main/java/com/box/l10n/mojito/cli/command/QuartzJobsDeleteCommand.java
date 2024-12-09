package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.apiclient.ApiClient;
import com.box.l10n.mojito.cli.apiclient.ApiException;
import com.box.l10n.mojito.cli.apiclient.QuartzWsApiProxy;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Parameters(
    commandNames = {"quartz-jobs-delete"},
    commandDescription = "Deletes all dynamic quartz jobs")
public class QuartzJobsDeleteCommand extends Command {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(QuartzJobsDeleteCommand.class);

  @Autowired ConsoleWriter consoleWriter;

  @Autowired ApiClient apiClient;

  QuartzWsApiProxy quartzJobsClient;

  @PostConstruct
  public void init() {
    this.quartzJobsClient = new QuartzWsApiProxy(this.apiClient);
  }

  @Override
  public boolean shouldShowInCommandList() {
    return false;
  }

  @Override
  protected void execute() throws CommandException {
    consoleWriter.a("Delete quartz jobs").println();
    try {
      this.quartzJobsClient.deleteAllDynamicJobs();
    } catch (ApiException e) {
      throw new CommandException(e.getMessage(), e);
    }
  }
}
