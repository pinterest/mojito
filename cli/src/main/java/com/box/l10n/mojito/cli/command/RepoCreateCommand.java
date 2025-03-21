package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.apiclient.LocaleClient;
import com.box.l10n.mojito.apiclient.exception.LocaleNotFoundException;
import com.box.l10n.mojito.apiclient.exception.ResourceNotCreatedException;
import com.box.l10n.mojito.apiclient.model.AssetIntegrityChecker;
import com.box.l10n.mojito.apiclient.model.Locale;
import com.box.l10n.mojito.apiclient.model.Repository;
import com.box.l10n.mojito.apiclient.model.RepositoryLocale;
import com.box.l10n.mojito.apiclient.model.RepositoryRepository;
import com.box.l10n.mojito.cli.command.param.Param;
import java.util.List;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author wyau
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"repo-create", "r"},
    commandDescription = "Creates a repository")
public class RepoCreateCommand extends RepoCommand {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(RepoCreateCommand.class);

  @Parameter(
      names = {Param.REPOSITORY_NAME_LONG, Param.REPOSITORY_NAME_SHORT},
      arity = 1,
      required = true,
      description = Param.REPOSITORY_NAME_DESCRIPTION)
  String nameParam;

  @Parameter(
      names = {Param.REPOSITORY_DESCRIPTION_LONG, Param.REPOSITORY_DESCRIPTION_SHORT},
      arity = 1,
      required = false,
      description = Param.REPOSITORY_DESCRIPTION_DESCRIPTION)
  String descriptionParam;

  @Parameter(
      names = {Param.CHECK_SLA_LONG, Param.CHECK_SLA_SHORT},
      arity = 1,
      required = false,
      description = Param.CHECK_SLA_DESCRIPTION)
  Boolean checkSLA = false;

  @Parameter(
      names = {Param.REPOSITORY_SOURCE_LOCALE_LONG, Param.REPOSITORY_SOURCE_LOCALE_SHORT},
      arity = 1,
      required = false,
      description = Param.REPOSITORY_SOURCE_LOCALE_DESCRIPTION)
  String sourceLocaleBcp47Tags = "en";

  /**
   * Each individual locales would be added. Bracket enclosed locale will set that locale to be
   * partially translated. Arrow (->) indicate inheritance, which the parent locale being
   * referenced.
   *
   * <p>Example: <code>
   * "fr-FR" "(fr-CA)->fr-FR" "en-GB" "(en-CA)->en-GB" "en-AU"
   * 1. Adds: fr-Fr, fr-CA, en-GB, en-CA, en-AU
   * 2. fr-CA and en-CA are children locale of fr-FR and en-GB and do not need to be fully translated
   * </code>
   */
  @Parameter(
      names = {Param.REPOSITORY_LOCALES_LONG, Param.REPOSITORY_LOCALES_SHORT},
      variableArity = true,
      required = true,
      description = Param.REPOSITORY_LOCALES_DESCRIPTION)
  List<String> encodedBcp47Tags;

  /**
   * Comma seperated by "FILE_EXTENSION:CHECKER_TYPE" "properties:message-format,xliff:sprintf"
   *
   * <p>For all available checker types:
   * com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckerType
   */
  @Parameter(
      names = {INTEGRITY_CHECK_LONG_PARAM, INTEGRITY_CHECK_SHORT_PARAM},
      arity = 1,
      required = false,
      description = INTEGRITY_CHECK_DESCRIPTION)
  String integrityCheckParam;

  @Autowired LocaleClient localeClient;

  @Override
  public void execute() throws CommandException {
    consoleWriter.a("Create repository: ").fg(Ansi.Color.CYAN).a(nameParam).println();

    try {
      List<RepositoryLocale> repositoryLocales =
          localeHelper.extractRepositoryLocalesFromInput(encodedBcp47Tags, true);
      List<AssetIntegrityChecker> integrityCheckers =
          extractIntegrityCheckersFromInput(integrityCheckParam, true);

      Locale sourceLocale = null;

      if (sourceLocaleBcp47Tags != null) {
        sourceLocale = localeClient.getLocaleByBcp47Tag(sourceLocaleBcp47Tags);
      }

      Repository repositoryBody = new Repository();
      repositoryBody.setName(nameParam);
      repositoryBody.setDescription(descriptionParam);
      repositoryBody.setSourceLocale(sourceLocale);
      repositoryBody.setRepositoryLocales(repositoryLocales);
      repositoryBody.setAssetIntegrityCheckers(integrityCheckers);
      repositoryBody.setCheckSLA(checkSLA);
      RepositoryRepository repository = repositoryClient.createRepository(repositoryBody);
      consoleWriter
          .newLine()
          .a("created --> repository id: ")
          .fg(Ansi.Color.MAGENTA)
          .a(repository.getId())
          .println();
    } catch (ParameterException | ResourceNotCreatedException | LocaleNotFoundException ex) {
      throw new CommandException(ex.getMessage(), ex);
    }
  }
}
