package com.box.l10n.mojito.cli.command;

import static com.box.l10n.mojito.cli.command.param.Param.BRANCH_CREATED_BEFORE_OPTIONS_AND_EXAMPLE;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.apiclient.PollableTaskClient;
import com.box.l10n.mojito.apiclient.RepositoryClient;
import com.box.l10n.mojito.apiclient.exception.PollableTaskException;
import com.box.l10n.mojito.apiclient.exception.RestClientException;
import com.box.l10n.mojito.apiclient.model.LocaleRepository;
import com.box.l10n.mojito.apiclient.model.PollableTask;
import com.box.l10n.mojito.apiclient.model.RepositoryLocaleRepository;
import com.box.l10n.mojito.apiclient.model.RepositoryRepository;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.cli.filefinder.FileFinder;
import com.box.l10n.mojito.cli.filefinder.FileFinderException;
import com.box.l10n.mojito.cli.filefinder.FileMatch;
import com.box.l10n.mojito.cli.filefinder.file.FileType;
import com.box.l10n.mojito.cli.filefinder.file.XcodeXliffFileType;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author wyau
 */
@Component
public class CommandHelper {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(CommandHelper.class);

  // Regex for timeframes (the placeholder will be replaced by all {@link TimeframeType} values)
  private static final String TIMEFRAMES_REGEX_PLACEHOLDER = "^[1-9][0-9]{0,2}[%s]$";

  private static final Map<TimeframeType, Function<Integer, Period>> TIMEFRAME_FUNCTIONS;

  /** Supported BOM */
  private final ByteOrderMark[] boms = {
    ByteOrderMark.UTF_8, ByteOrderMark.UTF_16BE, ByteOrderMark.UTF_16LE
  };

  @Autowired RepositoryClient repositoryClient;

  @Autowired PollableTaskClient pollableTaskClient;

  @Autowired ConsoleWriter consoleWriter;

  static {
    TIMEFRAME_FUNCTIONS = new HashMap<>();
    TIMEFRAME_FUNCTIONS.put(TimeframeType.DAYS, Period::ofDays);
    TIMEFRAME_FUNCTIONS.put(TimeframeType.WEEKS, Period::ofWeeks);
    TIMEFRAME_FUNCTIONS.put(TimeframeType.MONTHS, Period::ofMonths);
    TIMEFRAME_FUNCTIONS.put(TimeframeType.YEARS, Period::ofYears);
  }

  /**
   * @param repositoryName Name of repository
   * @return
   */
  public RepositoryRepository findRepositoryByName(String repositoryName) throws CommandException {

    try {
      Preconditions.checkNotNull(repositoryName, "Repository name can't be null");
      return repositoryClient.getRepositoryByName(repositoryName);
    } catch (RestClientException e) {
      throw new CommandException("Repository [" + repositoryName + "] is not found", e);
    }
  }

  /**
   * Get all repositories
   *
   * @return
   */
  public List<RepositoryRepository> getAllRepositories() {
    return repositoryClient.getRepositories(null);
  }

  /**
   * Get list of {@link com.box.l10n.mojito.cli.filefinder.FileMatch} from source directory
   *
   * @param commandDirectories
   * @param fileTypes
   * @param sourceLocale
   * @param sourcePathFilterRegex
   * @param directoriesIncludePatterns
   * @param directoriesExcludePatterns
   * @return
   * @throws CommandException
   */
  public ArrayList<FileMatch> getSourceFileMatches(
      CommandDirectories commandDirectories,
      List<FileType> fileTypes,
      String sourceLocale,
      String sourcePathFilterRegex,
      List<String> directoriesIncludePatterns,
      List<String> directoriesExcludePatterns)
      throws CommandException {
    logger.debug("Search for source asset to be localized");
    Stopwatch stopwatch = Stopwatch.createStarted();
    FileFinder fileFinder =
        getFileFinder(
            commandDirectories,
            fileTypes,
            sourceLocale,
            sourcePathFilterRegex,
            directoriesIncludePatterns,
            directoriesExcludePatterns);
    logger.info("Elapsed time scanning in getSourceFileMatches(): {}", stopwatch);
    return fileFinder.getSources();
  }

  /**
   * Get list of {@link com.box.l10n.mojito.cli.filefinder.FileMatch} from target directory
   *
   * @param commandDirectories
   * @param fileTypes
   * @param sourcePathFilterRegex
   * @return
   * @throws CommandException
   */
  public ArrayList<FileMatch> getTargetFileMatches(
      CommandDirectories commandDirectories,
      List<FileType> fileTypes,
      String sourceLocale,
      String sourcePathFilterRegex,
      List<String> directoriesIncludePatterns,
      List<String> directoriesExcludePatterns)
      throws CommandException {
    logger.debug("Search for target assets that are already localized");
    FileFinder fileFinder =
        getFileFinder(
            commandDirectories,
            fileTypes,
            sourceLocale,
            sourcePathFilterRegex,
            directoriesIncludePatterns,
            directoriesExcludePatterns);
    return fileFinder.getTargets();
  }

  /**
   * Get {@link FileFinder} from source directory
   *
   * @param commandDirectories
   * @param fileTypes
   * @param sourceLocale
   * @param sourcePathFilterRegex
   * @param directoriesIncludePatterns
   * @param directoriesExcludePatterns
   * @return
   * @throws CommandException
   */
  protected FileFinder getFileFinder(
      CommandDirectories commandDirectories,
      List<FileType> fileTypes,
      String sourceLocale,
      String sourcePathFilterRegex,
      List<String> directoriesIncludePatterns,
      List<String> directoriesExcludePatterns)
      throws CommandException {
    FileFinder fileFinder = new FileFinder();
    fileFinder.setSourceDirectory(commandDirectories.getSourceDirectoryPath());
    fileFinder.setTargetDirectory(commandDirectories.getTargetDirectoryPath());
    fileFinder.setDirectoriesIncludePattern(directoriesIncludePatterns);
    fileFinder.setDirectoriesExcludePattern(directoriesExcludePatterns);
    fileFinder.setSourcePathFilterRegex(sourcePathFilterRegex);

    if (fileTypes != null) {
      fileFinder.setFileTypes(fileTypes);
    }

    if (!Strings.isNullOrEmpty(sourceLocale)) {
      for (FileType fileTypeForUpdate : fileFinder.getFileTypes()) {
        fileTypeForUpdate.getLocaleType().setSourceLocale(sourceLocale);
      }
    }

    try {
      fileFinder.find();
    } catch (FileFinderException e) {
      throw new CommandException(e.getMessage(), e);
    }

    return fileFinder;
  }

  /**
   * Get content from {@link java.nio.file.Path} using UTF8
   *
   * @param path
   * @return
   * @throws CommandException
   */
  public String getFileContent(Path path) {
    try {
      File file = path.toFile();
      BOMInputStream inputStream = new BOMInputStream(FileUtils.openInputStream(file), false, boms);
      String fileContent;
      if (inputStream.hasBOM()) {
        fileContent = IOUtils.toString(inputStream, inputStream.getBOMCharsetName());
      } else {
        fileContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
      }
      return fileContent;
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot get file content for path: " + path.toString(), e);
    }
  }

  /**
   * Writes the content into a file using same format as source file
   *
   * @param content content to be written
   * @param path path to the file
   * @param sourceFileMatch
   * @throws CommandException
   */
  public void writeFileContent(String content, Path path, FileMatch sourceFileMatch)
      throws CommandException {
    writeFileContent(content, path, sourceFileMatch.getPath());
  }

  public void writeFileContent(String content, Path outputPath, Path inputFileForBOMCopy)
      throws CommandException {
    try {
      File outputFile = outputPath.toFile();
      BOMInputStream inputStream =
          new BOMInputStream(FileUtils.openInputStream(inputFileForBOMCopy.toFile()), false, boms);
      if (inputStream.hasBOM()) {
        FileUtils.writeByteArrayToFile(outputFile, inputStream.getBOM().getBytes());
        FileUtils.writeByteArrayToFile(
            outputFile, content.getBytes(inputStream.getBOMCharsetName()), true);
      } else {
        FileUtils.writeStringToFile(outputFile, content, StandardCharsets.UTF_8);
      }
    } catch (IOException e) {
      throw new CommandException("Cannot write file content in path: " + outputPath.toString(), e);
    }
  }

  /**
   * Writes the content into a file in UTF8
   *
   * @param content content to be written
   * @param path path to the file
   * @throws CommandException
   */
  public void writeFileContent(String content, Path path) throws CommandException {
    try {
      Files.write(content, path.toFile(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new CommandException("Cannot write file content in path: " + path.toString(), e);
    }
  }

  /**
   * Waits for {@link PollableTask} to be all finished (see {@link PollableTask#isAllFinished() }).
   * Infinite timeout.
   *
   * @param pollableId the {@link PollableTask#getId()}
   * @throws com.box.l10n.mojito.cli.command.CommandException
   */
  public void waitForPollableTask(Long pollableId) throws CommandException {

    consoleWriter
        .newLine()
        .a("Running, task id: ")
        .fg(Ansi.Color.MAGENTA)
        .a(pollableId)
        .a(" ")
        .println();

    try {
      pollableTaskClient.waitForPollableTask(
          pollableId, PollableTaskClient.NO_TIMEOUT, new CommandWaitForPollableTaskListener());
    } catch (PollableTaskException e) {
      throw new CommandException(e.getMessage(), e.getCause());
    }
  }

  public void waitForPollableTaskSilencedOutput(Long pollableId) throws CommandException {

    try {
      pollableTaskClient.waitForPollableTask(pollableId, PollableTaskClient.NO_TIMEOUT, null);
    } catch (PollableTaskException e) {
      throw new CommandException(e.getMessage(), e.getCause());
    }
  }

  /**
   * Gets the repository locales sorted so that parent are before child locales.
   *
   * @param repository
   * @return
   */
  public Map<String, LocaleRepository> getSortedRepositoryLocales(RepositoryRepository repository) {

    LinkedHashMap<String, LocaleRepository> locales = new LinkedHashMap<>();

    ArrayDeque<RepositoryLocaleRepository> toProcess =
        new ArrayDeque<>(repository.getRepositoryLocales());
    LocaleRepository rootLocale = null;

    for (RepositoryLocaleRepository rl : toProcess) {
      if (rl.getParentLocale() == null) {
        rootLocale = rl.getLocale();
        toProcess.remove(rl);
        break;
      }
    }

    Set<Long> localeIds = new HashSet<>();

    while (!toProcess.isEmpty()) {
      RepositoryLocaleRepository rl = toProcess.removeFirst();
      Long parentLocaleId = rl.getParentLocale().getLocale().getId();
      if (parentLocaleId.equals(rootLocale.getId()) || localeIds.contains(parentLocaleId)) {
        localeIds.add(rl.getLocale().getId());
        locales.put(rl.getLocale().getBcp47Tag(), rl.getLocale());
      } else {
        toProcess.addLast(rl);
      }
    }

    return locales;
  }

  /**
   * Returns the filter options provided or defaults to the file type filter options. (that can be
   * null too)
   *
   * @param fileType
   * @param filterOptions
   * @return the filter options provided or the default options for the file type (can be null)
   */
  public List<String> getFilterOptionsOrDefaults(FileType fileType, List<String> filterOptions) {
    return filterOptions == null ? fileType.getDefaultFilterOptions() : filterOptions;
  }

  /**
   * Returns the date of last week if the condition is true else {@code null}
   *
   * @param condition
   * @return
   */
  ZonedDateTime getLastWeekDateIfTrue(boolean condition) {
    ZonedDateTime dateTime = null;
    if (condition) {
      dateTime = JSR310Migration.dateTimeNowInUTC().minusWeeks(1);
    }
    return dateTime;
  }

  private static String getTimeframesRegexPlaceholder() {
    final StringJoiner timeframeRegexJoiner = new StringJoiner("|");
    Arrays.stream(TimeframeType.values())
        .forEach(
            timeframeType ->
                timeframeRegexJoiner
                    .add(String.valueOf(timeframeType.getAbbreviationInUpperCase()))
                    .add(String.valueOf(timeframeType.getAbbreviationInLowerCase())));
    return String.format(TIMEFRAMES_REGEX_PLACEHOLDER, timeframeRegexJoiner);
  }

  private static int getTimeframe(String input, TimeframeType timeframeType) {
    String regex =
        String.format(
            "\\d+(?=[%c|%c])",
            timeframeType.getAbbreviationInUpperCase(), timeframeType.getAbbreviationInLowerCase());
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(input);
    if (matcher.find()) {
      return Integer.parseInt(matcher.group());
    }
    return 0;
  }

  static ZonedDateTime getCreatedBeforeDateTime(final String timeframe) {
    if (!timeframe.matches(getTimeframesRegexPlaceholder())) {
      throw new CommandException(
          String.format(
              "Please enter a single valid timeframe %s",
              BRANCH_CREATED_BEFORE_OPTIONS_AND_EXAMPLE));
    }
    final List<Period> periods = new ArrayList<>();
    Arrays.stream(TimeframeType.values())
        .forEach(
            timeframeType -> {
              if (TIMEFRAME_FUNCTIONS.containsKey(timeframeType)) {
                periods.add(
                    TIMEFRAME_FUNCTIONS
                        .get(timeframeType)
                        .apply(getTimeframe(timeframe, timeframeType)));
              }
            });
    final Optional<Period> optionalPeriod =
        periods.stream().filter(period -> !period.isZero()).findFirst();
    final ZonedDateTime createdBeforeDateTime = JSR310Migration.dateTimeNowInUTC();
    return optionalPeriod.map(createdBeforeDateTime::minus).orElse(createdBeforeDateTime);
  }

  /**
   * Get content from {@link java.nio.file.Path} using UTF8 and if it is an XLIFF from XCode patch
   * the content (Adds attribute xml:space="preserve" in the trans-unit element in the xliff)
   *
   * @param sourceFileMatch
   * @return
   * @throws CommandException
   */
  public String getFileContentWithXcodePatch(FileMatch sourceFileMatch) {
    String assetContent = getFileContent(sourceFileMatch.getPath());

    // TODO(P1) This is to inject xml:space="preserve" in the trans-unit element
    // in the xcode-generated xliff until xcode fixes the bug of not adding this attribute
    // See Xcode bug http://www.openradar.me/23410569
    if (XcodeXliffFileType.class == sourceFileMatch.getFileType().getClass()) {
      assetContent =
          assetContent.replaceAll(
              "<trans-unit id=\"(.*?)\">", "<trans-unit id=\"$1\" xml:space=\"preserve\">");
    }

    return assetContent;
  }

  public String getMappedSourcePath(Map<String, String> assetMapping, String sourcePath) {
    if (assetMapping != null && assetMapping.get(sourcePath) != null) {
      String mapping = assetMapping.get(sourcePath);
      logger.debug("Use asset mapping from: {} to {}", sourcePath, mapping);
      sourcePath = mapping;
    }
    return sourcePath;
  }
}
