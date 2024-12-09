package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.apiclient.ApiClient;
import com.box.l10n.mojito.cli.apiclient.ApiException;
import com.box.l10n.mojito.cli.apiclient.QuartzWsApiProxy;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import jakarta.annotation.PostConstruct;
import java.util.List;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Parameters(
    commandNames = {"quartz-jobs-view"},
    commandDescription = "View all dynamic quartz jobs")
public class QuartzJobsViewCommand extends Command {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(QuartzJobsViewCommand.class);

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
    consoleWriter.a("Dynamic quartz jobs:").println();
    List<String> allDynamicJobs;
    try {
      allDynamicJobs = this.quartzJobsClient.getAllDynamicJobs();
    } catch (ApiException e) {
      throw new CommandException(e.getMessage(), e);
    }
    if (allDynamicJobs.isEmpty()) {
      consoleWriter.println().fg(Ansi.Color.CYAN).a("None").println();
    } else {
      allDynamicJobs.forEach(
          jobName -> {
            consoleWriter.fg(Ansi.Color.CYAN).a(jobName).println();
          });
    }

    consoleWriter.fg(Ansi.Color.GREEN).newLine().a("Finished").println(2);
  }
}
