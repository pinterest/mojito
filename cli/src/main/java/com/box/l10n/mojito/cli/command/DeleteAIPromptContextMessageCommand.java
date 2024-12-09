package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.apiclient.AiPromptWsApiProxy;
import com.box.l10n.mojito.cli.apiclient.ApiClient;
import com.box.l10n.mojito.cli.apiclient.ApiException;
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
    commandNames = {"delete-ai-prompt-context-message"},
    commandDescription = "Delete an AI prompt context message.")
public class DeleteAIPromptContextMessageCommand extends Command {

  static Logger logger = LoggerFactory.getLogger(DeleteAIPromptContextMessageCommand.class);

  @Parameter(
      names = {"--id", "-i"},
      required = true,
      description = "The id of the context message to delete.")
  Long id;

  @Autowired private ConsoleWriter consoleWriter;

  @Autowired private ApiClient apiClient;

  AiPromptWsApiProxy aiServiceClient;

  @PostConstruct
  public void init() {
    this.aiServiceClient = new AiPromptWsApiProxy(this.apiClient);
  }

  @Override
  protected void execute() throws CommandException {
    deletePromptContextMessage();
  }

  private void deletePromptContextMessage() {
    logger.debug("Received request to create prompt content message");
    try {
      this.aiServiceClient.deletePromptMessage(id);
    } catch (ApiException e) {
      throw new CommandException(e.getMessage(), e);
    }
    consoleWriter.newLine().a("Prompt context message deleted").println();
  }
}
