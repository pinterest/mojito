package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.apiclient.model.LocaleRepository;
import com.box.l10n.mojito.apiclient.model.RepositoryRepository;
import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.evolve.CourseDTO;
import com.box.l10n.mojito.evolve.EvolveClient;
import org.junit.Assume;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class EvolveCommandTest extends CLITestBase {

  static Logger logger = LoggerFactory.getLogger(EvolveCommandTest.class);

  @Autowired(required = false)
  EvolveClient evolveClient;

  @Test
  public void execute() throws Exception {
    Assume.assumeNotNull(evolveClient);
    Repository repository = createTestRepoUsingRepoService();
    getL10nJCommander().run("evolve-sync", "-r", repository.getName());
  }

  @Test
  public void writeJsonTo() {
    EvolveCommand evolveCommand = new EvolveCommand();
    evolveCommand.writeJsonTo = getTargetTestDir().toString();

    RepositoryRepository repository = new RepositoryRepository();
    repository.setName("evolveRepository");

    CourseDTO courseDTO = new CourseDTO();
    courseDTO.setId(1);

    LocaleRepository locale = new LocaleRepository();
    locale.setBcp47Tag("fr-FR");

    evolveCommand.writeJsonToFile(repository, courseDTO, locale, "{\"key\" : \"value\"}");
    checkExpectedGeneratedResources();
  }
}
