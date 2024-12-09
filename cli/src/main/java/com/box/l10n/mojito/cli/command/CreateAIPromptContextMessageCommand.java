package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.apiclient.AiPromptWsApiProxy;
import com.box.l10n.mojito.cli.apiclient.ApiClient;
import com.box.l10n.mojito.cli.apiclient.ApiException;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.cli.model.AIPromptContextMessageCreateRequest;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Parameters(
    commandNames = {"create-ai-prompt-context-message"},
    commandDescription = "Create an AI prompt context message.")
public class CreateAIPromptContextMessageCommand extends Command {

  static Logger logger = LoggerFactory.getLogger(CreateAIPromptContextMessageCommand.class);

  @Parameter(
      names = {"--content", "-c"},
      required = true,
      description = "The system prompt text")
  String content;

  @Parameter(
      names = {"--message-type", "-mt"},
      required = true,
      description = "The type of message to create")
  String messageType;

  @Parameter(
      names = {"--prompt-id", "-p"},
      required = true,
      description = "The id of the associated AI prompt.")
  Long promptId;

  @Parameter(
      names = {"--order-index", "-i"},
      required = true,
      description = "The index of the message in the prompt context.")
  int orderIndex;

  @Autowired private ConsoleWriter consoleWriter;

  @Autowired private ApiClient apiClient;

  AiPromptWsApiProxy aiServiceClient;

  @PostConstruct
  public void init() {
    this.aiServiceClient = new AiPromptWsApiProxy(this.apiClient);
  }

  @Override
  protected void execute() throws CommandException {
    createPromptContextMessage();
  }

  private AIPromptContextMessageCreateRequest getAiPromptContextMessageCreateRequest() {
    AIPromptContextMessageCreateRequest AIPromptContextMessageCreateRequest =
        new AIPromptContextMessageCreateRequest();
    AIPromptContextMessageCreateRequest.setContent(content);
    AIPromptContextMessageCreateRequest.setMessageType(messageType);
    AIPromptContextMessageCreateRequest.setAiPromptId(promptId);
    AIPromptContextMessageCreateRequest.setOrderIndex(orderIndex);
    return AIPromptContextMessageCreateRequest;
  }

  private void createPromptContextMessage() {
    logger.debug("Received request to create prompt content message");
    AIPromptContextMessageCreateRequest AIPromptContextMessageCreateRequest =
        getAiPromptContextMessageCreateRequest();
    long contextMessageId;
    try {
      contextMessageId =
          this.aiServiceClient.createPromptMessage(AIPromptContextMessageCreateRequest);
    } catch (ApiException e) {
      throw new CommandException(e.getMessage(), e);
    }
    consoleWriter
        .newLine()
        .a("Prompt context message created with id: " + contextMessageId)
        .println();
  }
}
