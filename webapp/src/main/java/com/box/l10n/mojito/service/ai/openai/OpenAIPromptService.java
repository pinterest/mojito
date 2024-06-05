package com.box.l10n.mojito.service.ai.openai;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.entity.AIPrompt;
import com.box.l10n.mojito.entity.AIPromptType;
import com.box.l10n.mojito.entity.PromptType;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryAIPrompt;
import com.box.l10n.mojito.rest.ai.AIException;
import com.box.l10n.mojito.rest.ai.AIPromptCreateRequest;
import com.box.l10n.mojito.service.ai.PromptService;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import io.micrometer.core.annotation.Timed;
import jakarta.transaction.Transactional;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "l10n.ai.service.type", havingValue = "OpenAI")
public class OpenAIPromptService implements PromptService {

  static Logger logger = LoggerFactory.getLogger(OpenAIPromptService.class);

  @Autowired RepositoryRepository repositoryRepository;

  @Autowired AIPromptRepository aiPromptRepository;

  @Autowired AIPromptTypeRepository aiPromptTypeRepository;

  @Autowired RepositoryAIPromptRepository repositoryAIPromptRepository;

  @Timed("OpenAIPromptService.createPrompt")
  @Transactional
  public Long createPrompt(AIPromptCreateRequest AIPromptCreateRequest) {

    Repository repository =
        repositoryRepository.findByName(AIPromptCreateRequest.getRepositoryName());

    if (repository == null) {
      logger.error("Repository not found: {}", AIPromptCreateRequest.getRepositoryName());
      throw new AIException("Repository not found: " + AIPromptCreateRequest.getRepositoryName());
    }

    AIPromptType aiPromptType =
        aiPromptTypeRepository.findByName(AIPromptCreateRequest.getPromptType());
    if (aiPromptType == null) {
      logger.error("Prompt type not found: {}", AIPromptCreateRequest.getPromptType());
      throw new AIException("Prompt type not found: " + AIPromptCreateRequest.getPromptType());
    }

    AIPrompt aiPrompt = new AIPrompt();
    aiPrompt.setSystemPrompt(AIPromptCreateRequest.getSystemPrompt());
    aiPrompt.setUserPrompt(AIPromptCreateRequest.getUserPrompt());
    aiPrompt.setPromptTemperature(AIPromptCreateRequest.getPromptTemperature());
    aiPrompt.setModelName(AIPromptCreateRequest.getModelName());
    aiPrompt.setCreatedDate(JSR310Migration.dateTimeNow());
    aiPromptRepository.save(aiPrompt);
    logger.debug("Created prompt with id: {}", aiPrompt.getId());

    RepositoryAIPrompt repositoryAIPrompt = new RepositoryAIPrompt();
    repositoryAIPrompt.setRepositoryId(repository.getId());
    repositoryAIPrompt.setAiPromptId(aiPrompt.getId());
    repositoryAIPrompt.setPromptTypeId(aiPromptType.getId());
    repositoryAIPromptRepository.save(repositoryAIPrompt);
    logger.debug("Created repository prompt with id: {}", repositoryAIPrompt.getId());

    return aiPrompt.getId();
  }

  @Timed("OpenAIPromptService.getPromptsByRepositoryAndPromptType")
  public List<AIPrompt> getPromptsByRepositoryAndPromptType(
      Repository repository, PromptType promptType) {
    return aiPromptRepository.findByRepositoryIdAndPromptTypeName(
        repository.getId(), promptType.name());
  }

  @Timed("OpenAIPromptService.deletePrompt")
  public void deletePrompt(Long promptId) {
    AIPrompt aiPrompt =
        aiPromptRepository
            .findById(promptId)
            .orElseThrow(() -> new AIException("Prompt not found: " + promptId));
    aiPrompt.setDeleted(true);
    aiPromptRepository.save(aiPrompt);
  }

  @Timed("OpenAIPromptService.getPrompt")
  public AIPrompt getPrompt(Long promptId) {
    return aiPromptRepository
        .findById(promptId)
        .orElseThrow(() -> new AIException("Prompt not found: " + promptId));
  }

  @Timed("OpenAIPromptService.getAllActivePrompts")
  public List<AIPrompt> getAllActivePrompts() {
    return aiPromptRepository.findByDeletedFalse();
  }

  @Timed("OpenAIService.getAllActivePromptsForRepository")
  public List<AIPrompt> getAllActivePromptsForRepository(String repositoryName) {
    Repository repository = repositoryRepository.findByName(repositoryName);
    if (repository == null) {
      logger.error("Repository not found: {}", repositoryName);
      throw new AIException("Repository not found: " + repositoryName);
    }
    return aiPromptRepository.findByRepositoryIdAndDeletedFalse(repository.getId());
  }
}
