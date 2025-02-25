package com.box.l10n.mojito.apiclient;

import com.box.l10n.mojito.cli.apiclient.RepositoryWsApi;
import com.box.l10n.mojito.cli.apiclient.model.BranchBranchSummary;
import com.box.l10n.mojito.cli.apiclient.model.Repository;
import com.box.l10n.mojito.cli.apiclient.model.RepositoryRepository;
import com.box.l10n.mojito.rest.client.exception.RepositoryNotFoundException;
import com.box.l10n.mojito.rest.client.exception.ResourceNotCreatedException;
import com.box.l10n.mojito.rest.client.exception.ResourceNotUpdatedException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Component
public class RepositoryClient {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(RepositoryClient.class);

  @Autowired private RepositoryWsApi repositoryWsApi;

  public List<BranchBranchSummary> getBranchesOfRepository(
      Long repositoryId,
      String branchName,
      String branchNameRegex,
      Boolean deleted,
      Boolean translated,
      boolean includeNullBranch,
      OffsetDateTime createdBefore) {
    List<BranchBranchSummary> branches =
        this.repositoryWsApi.getBranchesOfRepository(
            repositoryId, branchName, deleted, translated, createdBefore);
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
      throws RepositoryNotFoundException {
    logger.debug("Getting repo with name = {}", repositoryName);

    List<RepositoryRepository> repositoryList =
        this.repositoryWsApi.getRepositories(repositoryName);

    if (repositoryList.size() != 1) {
      throw new RepositoryNotFoundException(
          "Repository with name [" + repositoryName + "] is not found");
    }

    return repositoryList.getFirst();
  }

  /**
   * Deletes a {@link RepositoryRepository} by the {@link RepositoryRepository#getName()}
   *
   * @param repositoryName
   */
  public void deleteRepositoryByName(String repositoryName) throws RepositoryNotFoundException {
    logger.debug("Deleting repository by name = [{}]", repositoryName);
    RepositoryRepository repository = this.getRepositoryByName(repositoryName);
    this.repositoryWsApi.deleteRepositoryById(repository.getId());
  }

  public RepositoryRepository createRepository(Repository body) throws ResourceNotCreatedException {
    logger.debug(
        "Creating repo with name = {}, and description = {}, and repositoryLocales = {}",
        body.getName(),
        body.getDescription(),
        body.getRepositoryLocales().toString());
    try {
      return this.repositoryWsApi.createRepository(body);
    } catch (HttpClientErrorException exception) {
      if (exception.getStatusCode().equals(HttpStatus.CONFLICT)) {
        throw new ResourceNotCreatedException(exception.getResponseBodyAsString());
      } else {
        throw exception;
      }
    }
  }

  public void updateRepository(String name, Repository repositoryBody)
      throws ResourceNotUpdatedException, RepositoryNotFoundException {
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
      this.repositoryWsApi.updateRepository(updatedRepository, repository.getId());
    } catch (HttpClientErrorException exception) {
      if (exception.getStatusCode().equals(HttpStatus.CONFLICT)) {
        throw new ResourceNotUpdatedException(exception.getResponseBodyAsString());
      } else {
        throw exception;
      }
    }
  }

  public RepositoryRepository getRepositoryById(Long repositoryId) {
    logger.debug("Getting repository by id = [{}]", repositoryId);
    return this.repositoryWsApi.getRepositoryById(repositoryId);
  }
}
