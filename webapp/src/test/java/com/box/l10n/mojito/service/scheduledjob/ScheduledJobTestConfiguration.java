// package com.box.l10n.mojito.service.scheduledjob;
//
// import com.box.l10n.mojito.service.repository.RepositoryLocaleCreationException;
// import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
// import com.box.l10n.mojito.service.repository.RepositoryService;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.TestConfiguration;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Lazy;
//
// @TestConfiguration
// public class ScheduledJobTestConfiguration {
//  @Autowired RepositoryService repositoryService;
//
//  /**
//   * Create repository before initializing the ScheduledJobManager, this is required as the
//   * scheduled_job table requires a valid repository_id to be placed into the DB.
//   */
//  @Bean
//  @Lazy
//  public ScheduledJobManager scheduledJobManager()
//      throws RepositoryNameAlreadyUsedException, RepositoryLocaleCreationException {
//    for (int i = 1; i <= 4; i++) {
//      repositoryService.addRepositoryLocale(
//          repositoryService.createRepository("scheduled-job-" + i), "ro-RO");
//    }
//    return new ScheduledJobManager();
//  }
// }
