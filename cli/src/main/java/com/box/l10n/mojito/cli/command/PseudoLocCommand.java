package com.box.l10n.mojito.cli.command;

import static java.util.Optional.ofNullable;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.apiclient.AssetClient;
import com.box.l10n.mojito.apiclient.exception.AssetNotFoundException;
import com.box.l10n.mojito.apiclient.model.AssetAssetSummary;
import com.box.l10n.mojito.apiclient.model.LocalizedAssetBody;
import com.box.l10n.mojito.apiclient.model.RepositoryRepository;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.cli.filefinder.FileMatch;
import com.box.l10n.mojito.cli.filefinder.file.FileType;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.fusesource.jansi.Ansi.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Parameters(
    commandNames = {"pseudo", "pl"},
    commandDescription = "Pull pseudo localized assets from TMS")
public class PseudoLocCommand extends Command {

  public static final String OUTPUT_BCP47_TAG = "en-x-pseudo";

  /** logger */
  static Logger logger = LoggerFactory.getLogger(PseudoLocCommand.class);

  @Autowired ConsoleWriter consoleWriter;

  @Parameter(
      names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT},
      arity = 1,
      required = true,
      description = Param.REPOSITORY_DESCRIPTION)
  String repositoryParam;

  @Parameter(
      names = {Param.SOURCE_DIRECTORY_LONG, Param.SOURCE_DIRECTORY_SHORT},
      arity = 1,
      required = false,
      description = Param.SOURCE_DIRECTORY_DESCRIPTION)
  String sourceDirectoryParam;

  @Parameter(
      names = {Param.TARGET_DIRECTORY_LONG, Param.TARGET_DIRECTORY_SHORT},
      arity = 1,
      required = false,
      description = Param.TARGET_DIRECTORY_DESCRIPTION)
  String targetDirectoryParam;

  @Parameter(
      names = {Param.FILE_TYPE_LONG, Param.FILE_TYPE_SHORT},
      variableArity = true,
      required = false,
      description = Param.FILE_TYPE_DESCRIPTION,
      converter = FileTypeConverter.class)
  List<FileType> fileTypes;

  @Parameter(
      names = {Param.FILTER_OPTIONS_LONG, Param.FILTER_OPTIONS_SHORT},
      variableArity = true,
      required = false,
      description = Param.FILTER_OPTIONS_DESCRIPTION)
  List<String> filterOptionsParam;

  @Parameter(
      names = {Param.SOURCE_LOCALE_LONG, Param.SOURCE_LOCALE_SHORT},
      arity = 1,
      required = false,
      description = Param.SOURCE_LOCALE_DESCRIPTION)
  String sourceLocale;

  @Parameter(
      names = {Param.SOURCE_REGEX_LONG, Param.SOURCE_REGEX_SHORT},
      arity = 1,
      required = false,
      description = Param.SOURCE_REGEX_DESCRIPTION)
  String sourcePathFilterRegex;

  @Parameter(
      names = {Param.DIR_PATH_INCLUDE_PATTERNS_LONG},
      variableArity = true,
      required = false,
      description = Param.DIR_PATH_INCLUDE_PATTERNS_DESCRIPTION)
  List<String> directoriesIncludePatterns = null;

  @Parameter(
      names = {Param.DIR_PATH_EXCLUDE_PATTERNS_LONG},
      variableArity = true,
      required = false,
      description = Param.DIR_PATH_EXCLUDE_PATTERNS_DESCRIPTION)
  List<String> directoriesExcludePatterns = null;

  @Autowired AssetClient assetClient;

  @Autowired CommandHelper commandHelper;

  RepositoryRepository repository;

  CommandDirectories commandDirectories;

  /** Contains a map of locale for generating localized file a locales defined in the repository. */
  Map<String, String> localeMappings;

  @Override
  public void execute() throws CommandException {
    consoleWriter
        .newLine()
        .a("Pull pseudo localized asset from repository: ")
        .fg(Color.CYAN)
        .a(repositoryParam)
        .println(2);

    repository = commandHelper.findRepositoryByName(repositoryParam);

    commandDirectories = new CommandDirectories(sourceDirectoryParam, targetDirectoryParam);

    for (FileMatch sourceFileMatch :
        commandHelper.getSourceFileMatches(
            commandDirectories,
            fileTypes,
            sourceLocale,
            sourcePathFilterRegex,
            directoriesIncludePatterns,
            directoriesExcludePatterns)) {
      consoleWriter.a("Localizing: ").fg(Color.CYAN).a(sourceFileMatch.getSourcePath()).println();
      generatePseudoLocalizedFile(
          repository,
          sourceFileMatch,
          commandHelper.getFilterOptionsOrDefaults(
              sourceFileMatch.getFileType(), filterOptionsParam));
    }
    consoleWriter.fg(Color.GREEN).newLine().a("Finished").println(2);
  }

  /**
   * Generates the pseudo localized file
   *
   * @param repository
   * @param sourceFileMatch
   * @param filterOptions
   * @throws CommandException
   */
  void generatePseudoLocalizedFile(
      RepositoryRepository repository, FileMatch sourceFileMatch, List<String> filterOptions)
      throws CommandException {
    logger.debug("Generate pseudo localzied files");

    LocalizedAssetBody localizedAsset =
        getPseudoLocalizedAsset(repository, sourceFileMatch, filterOptions);
    writePseudoLocalizedAssetToTargetDirectory(localizedAsset, sourceFileMatch);
  }

  void writePseudoLocalizedAssetToTargetDirectory(
      LocalizedAssetBody localizedAsset, FileMatch sourceFileMatch) throws CommandException {
    localizedAsset.setBcp47Tag(OUTPUT_BCP47_TAG);

    Path targetPath =
        commandDirectories
            .getTargetDirectoryPath()
            .resolve(sourceFileMatch.getTargetPath(localizedAsset.getBcp47Tag()));

    commandHelper.writeFileContent(localizedAsset.getContent(), targetPath, sourceFileMatch);

    Path relativeTargetFilePath = commandDirectories.relativizeWithUserDirectory(targetPath);

    consoleWriter.a(" --> ").fg(Color.MAGENTA).a(relativeTargetFilePath.toString()).println();
  }

  LocalizedAssetBody getPseudoLocalizedAsset(
      RepositoryRepository repository, FileMatch sourceFileMatch, List<String> filterOptions)
      throws CommandException {
    consoleWriter.a(" - Processing locale: ").fg(Color.CYAN).a(OUTPUT_BCP47_TAG).print();

    try {
      AssetAssetSummary assetByPathAndRepositoryId =
          assetClient.getAssetByPathAndRepositoryId(
              sourceFileMatch.getSourcePath(), repository.getId());

      String assetContent = commandHelper.getFileContentWithXcodePatch(sourceFileMatch);

      LocalizedAssetBody localizedAssetBody = new LocalizedAssetBody();
      localizedAssetBody.setContent(assetContent);
      localizedAssetBody.setOutputBcp47tag(AssetClient.OUTPUT_BCP47_TAG);
      localizedAssetBody.setFilterConfigIdOverride(
          ofNullable(sourceFileMatch.getFileType().getFilterConfigIdOverride())
              .map(
                  filterConfigIdOverride ->
                      LocalizedAssetBody.FilterConfigIdOverrideEnum.fromValue(
                          filterConfigIdOverride.name()))
              .orElse(null));
      localizedAssetBody.setFilterOptions(filterOptions);
      LocalizedAssetBody pseudoLocalizedAsset =
          assetClient.getPseudoLocalizedAssetForContent(
              localizedAssetBody, assetByPathAndRepositoryId.getId());

      logger.trace("PseudoLocalizedAsset content = {}", pseudoLocalizedAsset.getContent());
      return pseudoLocalizedAsset;
    } catch (AssetNotFoundException e) {
      throw new CommandException(
          "Asset with path ["
              + sourceFileMatch.getSourcePath()
              + "] was not found in repo ["
              + repositoryParam
              + "]",
          e);
    }
  }
}
