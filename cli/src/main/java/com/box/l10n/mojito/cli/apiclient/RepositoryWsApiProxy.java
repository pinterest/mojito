package com.box.l10n.mojito.cli.apiclient;

import com.box.l10n.mojito.cli.apiclient.mappers.RepositoryMapper;
import com.box.l10n.mojito.cli.command.CommandException;
import com.box.l10n.mojito.cli.model.BranchBranchSummary;
import com.box.l10n.mojito.cli.model.ImportRepositoryBody;
import com.box.l10n.mojito.cli.model.Repository;
import com.box.l10n.mojito.cli.model.RepositoryRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

public class RepositoryWsApiProxy extends RepositoryWsApi {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(RepositoryWsApiProxy.class);

  public RepositoryWsApiProxy(ApiClient apiClient) {
    super(apiClient);
  }

  public List<BranchBranchSummary> getBranchesOfRepository(
      Long repositoryId,
      String branchName,
      String branchNameRegex,
      Boolean deleted,
      Boolean translated,
      boolean includeNullBranch,
      OffsetDateTime createdBefore)
      throws CommandException {
    List<BranchBranchSummary> branches;
    try {
      branches =
          this.getBranchesOfRepository(
              repositoryId, branchName, deleted, translated, createdBefore);
    } catch (ApiException e) {
      throw new CommandException(e.getMessage(), e);
    }
    if (branchNameRegex != null) {
      Pattern branchNamePattern = Pattern.compile(branchNameRegex);
      branches =
          branches.stream()
              .filter(
                  b -> {
                    if (b.getName() == null) {
                      return includeNullBranch;
                    } else {
                      return branchNamePattern.matcher(b.getName()).matches();
                    }
                  })
              .toList();
    }
    return branches;
  }

  public RepositoryRepository getRepositoryByName(String repositoryName)
      throws CommandException, ApiException {
    logger.debug("Getting repo with name = {}", repositoryName);

    List<RepositoryRepository> repositoryList = this.getRepositories(repositoryName);

    if (repositoryList.size() != 1) {
      throw new CommandException("Repository with name [" + repositoryName + "] is not found");
    }

    return repositoryList.getFirst();
  }

  /**
   * Deletes a {@link RepositoryRepository} by the {@link RepositoryRepository#getName()}
   *
   * @param repositoryName
   * @throws ApiException
   */
  public void deleteRepositoryByName(String repositoryName) throws ApiException {
    logger.debug("Deleting repository by name = [{}]", repositoryName);
    RepositoryRepository repository = this.getRepositoryByName(repositoryName);
    this.deleteRepositoryById(repository.getId());
  }

  public BranchBranchSummary getBranch(Long repositoryId, String branchName) {
    List<BranchBranchSummary> branches =
        this.getBranchesOfRepository(repositoryId, branchName, null, null, null, false, null);
    logger.debug("Support the \"null\" branch (name is null and param filtering doesn't work)");
    return branches.stream()
        .filter((b) -> Objects.equals(b.getName(), branchName))
        .findFirst()
        .orElse(null);
  }

  @Override
  public RepositoryRepository createRepository(Repository body)
      throws ApiException, CommandException {
    logger.debug(
        "Creating repo with name = {}, and description = {}, and repositoryLocales = {}",
        body.getName(),
        body.getDescription(),
        body.getRepositoryLocales().toString());
    try {
      return super.createRepository(body);
    } catch (ApiException exception) {
      if (exception.getCode() == HttpStatus.CONFLICT.value()) {
        throw new CommandException(exception.getResponseBody());
      } else {
        throw exception;
      }
    }
  }

  @Override
  public String importRepository(ImportRepositoryBody body, Long repositoryId)
      throws ApiException, CommandException {
    try {
      return super.importRepository(body, repositoryId);
    } catch (ApiException exception) {
      if (exception.getCode() == HttpStatus.CONFLICT.value()) {
        throw new CommandException("Importing to repository [" + repositoryId + "] failed");
      } else {
        throw exception;
      }
    }
  }

  public void updateRepository(String name, Repository repositoryBody)
      throws ApiException, CommandException {
    logger.debug("Updating repository by name = [{}]", name);
    RepositoryRepository repository = this.getRepositoryByName(name);
    Repository updatedRepository = RepositoryMapper.mapToRepository(repository);
    updatedRepository.setDescription(repositoryBody.getDescription());
    updatedRepository.setName(repositoryBody.getName());
    updatedRepository.setRepositoryLocales(repositoryBody.getRepositoryLocales());
    updatedRepository.setCheckSLA(repositoryBody.isCheckSLA());
    if (repositoryBody.getAssetIntegrityCheckers() != null) {
      updatedRepository.setAssetIntegrityCheckers(repositoryBody.getAssetIntegrityCheckers());
    }
    try {
      this.updateRepository(updatedRepository, repository.getId());
    } catch (ApiException exception) {
      if (exception.getCode() == HttpStatus.CONFLICT.value()) {
        throw new CommandException(exception.getResponseBody());
      } else {
        throw exception;
      }
    }
  }
}
