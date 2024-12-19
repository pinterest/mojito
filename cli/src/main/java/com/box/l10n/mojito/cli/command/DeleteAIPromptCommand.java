package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.apiclient.AiPromptWsApiProxy;
import com.box.l10n.mojito.cli.apiclient.ApiClient;
import com.box.l10n.mojito.cli.apiclient.ApiException;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import jakarta.annotation.PostConstruct;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Parameters(
    commandNames = {"delete-ai-prompt"},
    commandDescription = "Delete an AI Prompt")
public class DeleteAIPromptCommand extends Command {

  static Logger logger = LoggerFactory.getLogger(DeleteAIPromptCommand.class);

  @Parameter(
      names = {"--prompt-id", "-pi"},
      required = true,
      description = "Prompt id")
  Long promptId;

  @Autowired private ConsoleWriter consoleWriter;

  @Autowired private ApiClient apiClient;

  AiPromptWsApiProxy aiServiceClient;

  @PostConstruct
  public void init() {
    this.aiServiceClient = new AiPromptWsApiProxy(this.apiClient);
  }

  @Override
  protected void execute() throws CommandException {
    consoleWriter
        .fg(Ansi.Color.YELLOW)
        .newLine()
        .a("Deleting AI Prompt with id: " + promptId)
        .println();
    deletePrompt();
  }

  private void deletePrompt() {
    logger.debug("Received request to delete prompt {}", promptId);
    try {
      this.aiServiceClient.deletePrompt(promptId);
    } catch (ApiException e) {
      throw new CommandException(e.getMessage(), e);
    }
  }
}
