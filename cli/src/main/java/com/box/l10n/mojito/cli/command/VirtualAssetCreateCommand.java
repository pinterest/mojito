package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.apiclient.ApiException;
import com.box.l10n.mojito.cli.apiclient.RepositoryWsApiHelper;
import com.box.l10n.mojito.cli.apiclient.VirtualAssetWsApi;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.cli.model.RepositoryRepository;
import com.box.l10n.mojito.cli.model.VirtualAsset;
import com.box.l10n.mojito.rest.client.HttpClientErrorJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"virtual-asset-create"},
    commandDescription = "Create an asset (with virtual content)")
public class VirtualAssetCreateCommand extends Command {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(VirtualAssetCreateCommand.class);

  @Autowired ConsoleWriter consoleWriter;

  @Parameter(
      names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT},
      arity = 1,
      required = true,
      description = Param.REPOSITORY_DESCRIPTION)
  String repositoryParam;

  @Parameter(
      names = {"--path", "-p"},
      arity = 1,
      required = true,
      description = Param.REPOSITORY_DESCRIPTION)
  String pathParam;

  @Autowired VirtualAssetWsApi virtualAssetClient;

  @Autowired RepositoryWsApiHelper repositoryWsApiHelper;

  private HttpClientErrorJson toHttpClientErrorJson(ApiException ae) {
    try {
      return new ObjectMapper().readValue(ae.getResponseBody(), HttpClientErrorJson.class);
    } catch (IOException ioe) {
      throw new RuntimeException("Can't convert HttpClientErrorException to Json", ioe);
    }
  }

  @Override
  public void execute() throws CommandException {

    consoleWriter
        .newLine()
        .a("Create a virtual asset: ")
        .fg(Ansi.Color.CYAN)
        .a(pathParam)
        .reset()
        .a(" in repository: ")
        .fg(Ansi.Color.CYAN)
        .a(repositoryParam)
        .println(2);

    RepositoryRepository repository =
        this.repositoryWsApiHelper.findRepositoryByName(repositoryParam);

    VirtualAsset virtualAsset = new VirtualAsset();
    virtualAsset.setPath(pathParam);
    virtualAsset.setRepositoryId(repository.getId());
    virtualAsset.setDeleted(Boolean.FALSE);

    try {
      consoleWriter.a(" - Create virtual asset: ").fg(Ansi.Color.CYAN).a(pathParam).println();
      virtualAsset = virtualAssetClient.createOrUpdateVirtualAsset(virtualAsset);
      consoleWriter.a(" --> asset id: ").fg(Ansi.Color.MAGENTA).a(virtualAsset.getId()).println();
      consoleWriter.fg(Ansi.Color.GREEN).newLine().a("Finished").println(2);
    } catch (ApiException ae) {
      HttpClientErrorJson toHttpClientErrorJson = this.toHttpClientErrorJson(ae);
      consoleWriter.println().fg(Ansi.Color.RED).a(toHttpClientErrorJson.getMessage()).println(2);
    }
  }
}
