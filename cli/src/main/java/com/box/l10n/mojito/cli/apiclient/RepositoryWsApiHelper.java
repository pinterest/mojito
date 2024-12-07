package com.box.l10n.mojito.cli.apiclient;

import com.box.l10n.mojito.cli.command.CommandException;
import com.box.l10n.mojito.cli.model.BranchBranchSummary;
import com.box.l10n.mojito.cli.model.RepositoryRepository;
import com.google.common.base.Preconditions;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RepositoryWsApiHelper {
  @Autowired RepositoryWsApi repositoryClient;

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
          this.repositoryClient.getBranchesOfRepository(
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

  public RepositoryRepository findRepositoryByName(String repositoryName) throws CommandException {
    try {
      Preconditions.checkNotNull(repositoryName, "Repository name can't be null");
      List<RepositoryRepository> repositories = repositoryClient.getRepositories(repositoryName);
      if (repositories.size() != 1) {
        throw new CommandException("Repository with name [" + repositoryName + "] is not found");
      }
      return repositories.getFirst();
    } catch (ApiException e) {
      throw new CommandException("Repository [" + repositoryName + "] is not found", e);
    }
  }
}
