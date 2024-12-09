package com.box.l10n.mojito.cli.apiclient.mappers;

import static java.util.Optional.ofNullable;

import com.box.l10n.mojito.cli.model.Branch;
import com.box.l10n.mojito.cli.model.BranchGitBlameWithUsage;
import com.box.l10n.mojito.cli.model.GitBlame;
import com.box.l10n.mojito.cli.model.GitBlameGitBlameWithUsage;
import com.box.l10n.mojito.cli.model.GitBlameWithUsage;
import com.box.l10n.mojito.cli.model.GitBlameWithUsageGitBlameWithUsage;
import com.box.l10n.mojito.cli.model.Repository;
import com.box.l10n.mojito.cli.model.RepositoryGitBlameWithUsage;
import com.box.l10n.mojito.cli.model.Screenshot;
import com.box.l10n.mojito.cli.model.ScreenshotGitBlameWithUsage;
import com.box.l10n.mojito.cli.model.ScreenshotTextUnit;
import com.box.l10n.mojito.cli.model.ScreenshotTextUnitGitBlameWithUsage;
import com.box.l10n.mojito.cli.model.TMTextUnit;
import com.box.l10n.mojito.cli.model.TMTextUnitGitBlameWithUsage;
import com.box.l10n.mojito.cli.model.User;
import com.box.l10n.mojito.cli.model.UserGitBlameWithUsage;

public class GitBlameWithUsageMapper {
  public static GitBlame mapToGitBlame(GitBlameGitBlameWithUsage gitBlameWithUsage) {
    GitBlame gitBlame = new GitBlame();
    gitBlame.setId(gitBlameWithUsage.getId());
    gitBlame.setCreatedDate(gitBlameWithUsage.getCreatedDate());
    gitBlame.setAuthorEmail(gitBlameWithUsage.getAuthorEmail());
    gitBlame.setAuthorName(gitBlameWithUsage.getAuthorName());
    gitBlame.setCommitTime(gitBlameWithUsage.getCommitTime());
    gitBlame.setCommitName(gitBlameWithUsage.getCommitName());
    return gitBlame;
  }

  public static Repository mapToRepository(
      RepositoryGitBlameWithUsage repositoryGitBlameWithUsage) {
    Repository repository = new Repository();
    repository.setId(repositoryGitBlameWithUsage.getId());
    repository.setCreatedDate(repositoryGitBlameWithUsage.getCreatedDate());
    repository.setName(repositoryGitBlameWithUsage.getName());
    return repository;
  }

  public static User mapToUser(UserGitBlameWithUsage userGitBlameWithUsage) {
    User user = new User();
    user.setId(userGitBlameWithUsage.getId());
    user.setCreatedDate(userGitBlameWithUsage.getCreatedDate());
    user.setUsername(userGitBlameWithUsage.getUsername());
    user.setCommonName(userGitBlameWithUsage.getCommonName());
    return user;
  }

  public static TMTextUnit mapToTmTextUnit(
      TMTextUnitGitBlameWithUsage tmTextUnitGitBlameWithUsage) {
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setId(tmTextUnitGitBlameWithUsage.getId());
    tmTextUnit.setCreatedDate(tmTextUnitGitBlameWithUsage.getCreatedDate());
    tmTextUnit.setName(tmTextUnitGitBlameWithUsage.getName());
    return tmTextUnit;
  }

  public static ScreenshotTextUnit mapToScreenshotTextUnit(
      ScreenshotTextUnitGitBlameWithUsage screenshotTextUnitGitBlameWithUsage) {
    ScreenshotTextUnit screenshotTextUnit = new ScreenshotTextUnit();
    screenshotTextUnit.setId(screenshotTextUnitGitBlameWithUsage.getId());
    screenshotTextUnit.setTmTextUnit(
        ofNullable(screenshotTextUnitGitBlameWithUsage.getTmTextUnit())
            .map(GitBlameWithUsageMapper::mapToTmTextUnit)
            .orElse(null));
    return screenshotTextUnit;
  }

  public static Screenshot mapToScreenshot(
      ScreenshotGitBlameWithUsage screenshotGitBlameWithUsage) {
    Screenshot screenshot = new Screenshot();
    screenshot.setId(screenshotGitBlameWithUsage.getId());
    screenshot.setCreatedDate(screenshotGitBlameWithUsage.getCreatedDate());
    screenshot.setSrc(screenshotGitBlameWithUsage.getSrc());
    screenshot.setTextUnits(
        ofNullable(screenshotGitBlameWithUsage.getTextUnits())
            .map(
                textUnits ->
                    textUnits.stream()
                        .map(GitBlameWithUsageMapper::mapToScreenshotTextUnit)
                        .toList())
            .orElse(null));
    return screenshot;
  }

  public static Branch mapToBranch(BranchGitBlameWithUsage branchGitBlameWithUsage) {
    Branch branch = new Branch();
    branch.setId(branchGitBlameWithUsage.getId());
    branch.setCreatedDate(branchGitBlameWithUsage.getCreatedDate());
    branch.setRepository(
        ofNullable(branchGitBlameWithUsage.getRepository())
            .map(GitBlameWithUsageMapper::mapToRepository)
            .orElse(null));
    branch.setName(branchGitBlameWithUsage.getName());
    branch.setCreatedByUser(
        ofNullable(branchGitBlameWithUsage.getCreatedByUser())
            .map(GitBlameWithUsageMapper::mapToUser)
            .orElse(null));
    branch.setDeleted(branchGitBlameWithUsage.isDeleted());
    branch.setScreenshots(
        ofNullable(branchGitBlameWithUsage.getScreenshots())
            .map(
                screenshots ->
                    screenshots.stream().map(GitBlameWithUsageMapper::mapToScreenshot).toList())
            .orElse(null));
    return branch;
  }

  public static GitBlameWithUsage mapToGitBlameWithUsage(
      GitBlameWithUsageGitBlameWithUsage gitBlameWithUsage) {
    GitBlameWithUsage newGitBlameWithUsage = new GitBlameWithUsage();
    newGitBlameWithUsage.setUsages(gitBlameWithUsage.getUsages());
    newGitBlameWithUsage.setTextUnitName(gitBlameWithUsage.getTextUnitName());
    newGitBlameWithUsage.setPluralForm(gitBlameWithUsage.getPluralForm());
    newGitBlameWithUsage.setTmTextUnitId(gitBlameWithUsage.getTmTextUnitId());
    newGitBlameWithUsage.setAssetId(gitBlameWithUsage.getAssetId());
    newGitBlameWithUsage.setAssetTextUnitId(gitBlameWithUsage.getAssetTextUnitId());
    newGitBlameWithUsage.setThirdPartyTextUnitId(gitBlameWithUsage.getThirdPartyTextUnitId());
    newGitBlameWithUsage.setContent(gitBlameWithUsage.getContent());
    newGitBlameWithUsage.setComment(gitBlameWithUsage.getComment());
    newGitBlameWithUsage.setGitBlame(
        ofNullable(gitBlameWithUsage.getGitBlame())
            .map(GitBlameWithUsageMapper::mapToGitBlame)
            .orElse(null));
    newGitBlameWithUsage.setBranch(
        ofNullable(gitBlameWithUsage.getBranch())
            .map(GitBlameWithUsageMapper::mapToBranch)
            .orElse(null));
    newGitBlameWithUsage.setScreenshots(
        ofNullable(gitBlameWithUsage.getScreenshots())
            .map(
                screenshots ->
                    screenshots.stream().map(GitBlameWithUsageMapper::mapToScreenshot).toList())
            .orElse(null));
    newGitBlameWithUsage.setIntroducedBy(gitBlameWithUsage.getIntroducedBy());
    newGitBlameWithUsage.setIsVirtual(gitBlameWithUsage.isIsVirtual());
    return newGitBlameWithUsage;
  }
}
