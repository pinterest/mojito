package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.apiclient.ApiClient;
import com.box.l10n.mojito.cli.apiclient.ApiException;
import com.box.l10n.mojito.cli.apiclient.DropWsApi;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.Console;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.cli.model.DropDropSummary;
import com.box.l10n.mojito.cli.model.ImportDropConfig;
import com.box.l10n.mojito.cli.model.Pageable;
import com.box.l10n.mojito.cli.model.PollableTask;
import com.box.l10n.mojito.cli.model.RepositoryRepository;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.fusesource.jansi.Ansi.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Command to import a drop. Displays the list of drops available and ask the user the drop id to be
 * imported.
 *
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"drop-import"},
    commandDescription = "Import a translated drop")
public class DropImportCommand extends Command {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(DropImportCommand.class);

  @Autowired ConsoleWriter consoleWriter;

  @Parameter(
      names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT},
      arity = 1,
      required = true,
      description = Param.REPOSITORY_DESCRIPTION)
  String repositoryParam;

  @Parameter(
      names = {"--number-drops-fetched"},
      arity = 1,
      required = false,
      description = "Number of drops fetched")
  Long numberOfDropsToFetchParam = 10L;

  @Parameter(
      names = {"--show-all", "-all"},
      required = false,
      description = "Show all drops (already imported drops are hidden by default)")
  Boolean alsoShowImportedParam = false;

  @Parameter(
      names = {Param.DROP_IMPORT_STATUS},
      required = false,
      description = Param.DROP_IMPORT_STATUS_DESCRIPTION,
      converter = ImportDropConfigStatusConverter.class)
  ImportDropConfig.StatusEnum importStatusParam = null;

  @Parameter(
      names = {"--import-fetched"},
      required = false,
      description = "Import all fetched drops")
  Boolean importFetchedParam = false;

  @Autowired CommandHelper commandHelper;

  @Autowired Console console;

  @Autowired ApiClient apiClient;

  DropWsApi dropClient;

  @PostConstruct
  public void init() {
    this.dropClient = new DropWsApi(this.apiClient);
  }

  private ImportDropConfig getImportDropConfig(Long dropId, RepositoryRepository repository) {
    ImportDropConfig importDropConfigBody = new ImportDropConfig();
    importDropConfigBody.setRepositoryId(repository.getId());
    importDropConfigBody.setDropId(dropId);
    importDropConfigBody.setStatus(importStatusParam);
    return importDropConfigBody;
  }

  @Override
  public void execute() throws CommandException {

    RepositoryRepository repository;
    try {
      repository = this.commandHelper.findRepositoryByName(repositoryParam);
    } catch (ApiException e) {
      throw new CommandException(e.getMessage(), e);
    }

    Map<Long, DropDropSummary> numberedAvailableDrops =
        getNumberedAvailableDrops(repository.getId());

    if (numberedAvailableDrops.isEmpty()) {
      consoleWriter.newLine().a("No drop available").println();
    } else {
      consoleWriter.newLine().a("Drops available").println();

      logger.debug("Display drops information");
      for (Map.Entry<Long, DropDropSummary> entry : numberedAvailableDrops.entrySet()) {

        DropDropSummary drop = entry.getValue();

        consoleWriter
            .a("  ")
            .fg(Color.CYAN)
            .a(entry.getKey())
            .reset()
            .a(" - id: ")
            .fg(Color.MAGENTA)
            .a(drop.getId())
            .reset()
            .a(", name: ")
            .fg(Color.MAGENTA)
            .a(drop.getName())
            .reset();

        if (Boolean.TRUE.equals(drop.isCanceled())) {
          consoleWriter.fg(Color.GREEN).a(" CANCELED");
        } else if (drop.getLastImportedDate() == null) {
          consoleWriter.fg(Color.GREEN).a(" NEW");
        } else {
          consoleWriter.a(", last import: ").fg(Color.MAGENTA).a(drop.getLastImportedDate());
        }

        consoleWriter.println();
      }

      List<Long> dropIds = getSelectedDropIds(numberedAvailableDrops);

      for (Long dropId : dropIds) {
        consoleWriter
            .newLine()
            .a("Import drop: ")
            .fg(Color.CYAN)
            .a(dropId)
            .reset()
            .a(" in repository: ")
            .fg(Color.CYAN)
            .a(repositoryParam)
            .println(2);

        ImportDropConfig importDropConfigBody = getImportDropConfig(dropId, repository);
        ImportDropConfig importDropConfig;
        try {
          importDropConfig = dropClient.importDrop(importDropConfigBody);
        } catch (ApiException e) {
          throw new RuntimeException(e);
        }
        PollableTask pollableTask = importDropConfig.getPollableTask();

        commandHelper.waitForPollableTask(pollableTask.getId());
      }
    }

    consoleWriter.newLine().fg(Color.GREEN).a("Finished").println(2);
  }

  /**
   * Gets available {@link com.box.l10n.mojito.cli.model.DropDropSummary}s and assign them a number
   * (map key) to be referenced in the console input for selection.
   *
   * @return
   */
  private Map<Long, DropDropSummary> getNumberedAvailableDrops(Long repositoryId) {

    logger.debug("Build a map of drops keyed by an incremented integer");
    Map<Long, DropDropSummary> dropIds = new HashMap<>();

    long i = 1;

    Pageable pageable = new Pageable();
    pageable.setPage(0);
    pageable.setSize(this.numberOfDropsToFetchParam.intValue());
    try {
      for (DropDropSummary availableDrop :
          dropClient.getDrops(pageable, repositoryId, getImportedFilter(), null).getContent()) {
        dropIds.put(i++, availableDrop);
      }
    } catch (ApiException e) {
      throw new CommandException(e.getMessage(), e);
    }

    return dropIds;
  }

  /**
   * Returns the "imported" filter to be passed to {@link
   * com.box.l10n.mojito.cli.apiclient.DropWsApi#getDrops(com.box.l10n.mojito.cli.model.Pageable,
   * Long, Boolean, Boolean)} based on the CLI parameter {@link #alsoShowImportedParam}.
   *
   * @return the imported filter to get drops
   */
  private Boolean getImportedFilter() {
    return alsoShowImportedParam ? null : false;
  }

  /**
   * Gets the list of selected {@link com.box.l10n.mojito.cli.model.DropDropSummary#getId()}.
   *
   * <p>First, reads a drop number from the console and then gets the {@link
   * com.box.l10n.mojito.cli.model.DropDropSummary} from the map of available {@link
   * com.box.l10n.mojito.cli.model.DropDropSummary}s.
   *
   * @param numberedAvailableDrops candidate {@link com.box.l10n.mojito.cli.model.DropDropSummary}s
   *     for selection
   * @return selected {@link com.box.l10n.mojito.cli.model.DropDropSummary#getId()}
   * @throws CommandException if the input doesn't match a number from the map of available {@link
   *     com.box.l10n.mojito.cli.model.DropDropSummary}s
   */
  private List<Long> getSelectedDropIds(Map<Long, DropDropSummary> numberedAvailableDrops)
      throws CommandException {
    List<Long> selectedDropIds;

    if (importFetchedParam) {
      selectedDropIds = getWithImportFetchedDropIds(numberedAvailableDrops);
    } else {
      selectedDropIds = getFromConsoleDropIds(numberedAvailableDrops);
    }

    return selectedDropIds;
  }

  private List<Long> getFromConsoleDropIds(Map<Long, DropDropSummary> numberedAvailableDrops)
      throws CommandException {
    consoleWriter.newLine().a("Enter Drop number to import").println();
    Long dropNumber = console.readLine(Long.class);

    if (!numberedAvailableDrops.containsKey(dropNumber)) {
      throw new CommandException(
          "Please enter a number from the list: " + numberedAvailableDrops.keySet());
    }

    Long selectId = numberedAvailableDrops.get(dropNumber).getId();

    return Arrays.asList(selectId);
  }

  private List<Long> getWithImportFetchedDropIds(
      Map<Long, DropDropSummary> numberedAvailableDrops) {
    return numberedAvailableDrops.entrySet().stream()
        .filter(x -> !Boolean.TRUE.equals(x.getValue().isCanceled()))
        .map(x -> x.getValue().getId())
        .collect(Collectors.toList());
  }
}
