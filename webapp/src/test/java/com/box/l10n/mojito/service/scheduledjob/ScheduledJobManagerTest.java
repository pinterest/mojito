package com.box.l10n.mojito.service.scheduledjob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.box.l10n.mojito.entity.ScheduledJob;
import com.box.l10n.mojito.quartz.QuartzSchedulerManager;
import com.box.l10n.mojito.retry.DeadLockLoserExceptionRetryTemplate;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.repository.RepositoryLocaleCreationException;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.thirdparty.ThirdPartySyncJobConfig;
import com.box.l10n.mojito.service.thirdparty.ThirdPartySyncJobsConfig;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;

public class ScheduledJobManagerTest extends ServiceTestBase {

  private static boolean initializedDB = false;
  private ScheduledJobManager scheduledJobManager;
  @Autowired RepositoryService repositoryService;
  @Autowired QuartzSchedulerManager schedulerManager;
  @Autowired ScheduledJobRepository scheduledJobRepository;
  @Autowired ScheduledJobStatusRepository scheduledJobStatusRepository;
  @Autowired ScheduledJobTypeRepository scheduledJobTypeRepository;
  @Autowired RepositoryRepository repositoryRepository;
  @Autowired DeadLockLoserExceptionRetryTemplate deadlockRetryTemplate;

  @Before
  public void initialize()
      throws RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          ClassNotFoundException,
          SchedulerException {

    if (!initializedDB) {
      // Flyway not used in HSQL, do the manually insertions flyway would do
      for (int i = 1; i <= 4; i++) {
        if (repositoryService
            .findRepositoriesIsNotDeletedOrderByName("scheduled-job-" + i)
            .isEmpty()) {
          repositoryService.addRepositoryLocale(
              repositoryService.createRepository("scheduled-job-" + i), "fr-FR");
        }
      }

      com.box.l10n.mojito.entity.ScheduledJobType scheduledJobTypeEntity =
          new com.box.l10n.mojito.entity.ScheduledJobType();
      scheduledJobTypeEntity.setEnum(ScheduledJobType.THIRD_PARTY_SYNC);
      scheduledJobTypeRepository.save(scheduledJobTypeEntity);

      List.of(
              ScheduledJobStatus.SCHEDULED,
              ScheduledJobStatus.IN_PROGRESS,
              ScheduledJobStatus.FAILED,
              ScheduledJobStatus.SUCCEEDED)
          .forEach(
              status -> {
                com.box.l10n.mojito.entity.ScheduledJobStatus scheduledJobStatusEntity =
                    new com.box.l10n.mojito.entity.ScheduledJobStatus();
                scheduledJobStatusEntity.setJobStatus(status);
                scheduledJobStatusRepository.save(scheduledJobStatusEntity);
              });
      initializedDB = true;
    }

    ThirdPartySyncJobConfig scheduledJobOne = new ThirdPartySyncJobConfig();
    scheduledJobOne.setUuid("e4c72563-d8f0-4465-9eec-9ed96087665e");
    scheduledJobOne.setCron("0/5 * * * * ?");
    scheduledJobOne.setRepository("scheduled-job-1");
    scheduledJobOne.setThirdPartyProjectId("123456");
    scheduledJobOne.setActions(List.of());
    scheduledJobOne.setPluralSeparator("_");
    scheduledJobOne.setLocaleMapping("");
    scheduledJobOne.setSkipTextUnitsWithPattern("");
    scheduledJobOne.setSkipAssetsWithPathPattern("");
    scheduledJobOne.setIncludeTextUnitsWithPattern("");
    scheduledJobOne.setOptions(List.of());

    ThirdPartySyncJobConfig scheduledJobTwo = new ThirdPartySyncJobConfig();
    scheduledJobTwo.setUuid("e4c72563-d8f0-4465-9eec-aaa96087665e");
    scheduledJobTwo.setCron("0/5 * * * * ?");
    scheduledJobTwo.setRepository("scheduled-job-2");
    scheduledJobTwo.setThirdPartyProjectId("123456");
    scheduledJobTwo.setActions(List.of());
    scheduledJobTwo.setPluralSeparator("_");
    scheduledJobTwo.setLocaleMapping("");
    scheduledJobTwo.setSkipTextUnitsWithPattern("");
    scheduledJobTwo.setSkipAssetsWithPathPattern("");
    scheduledJobTwo.setIncludeTextUnitsWithPattern("");
    scheduledJobTwo.setOptions(List.of());

    ThirdPartySyncJobsConfig thirdPartySyncJobsConfig = new ThirdPartySyncJobsConfig();
    thirdPartySyncJobsConfig.setThirdPartySyncJobs(
        ImmutableMap.of("scheduled-job-1", scheduledJobOne, "scheduled-job-2", scheduledJobTwo));

    // Return a Spy with the Job always being a no-op job
    scheduledJobManager =
        Mockito.spy(
            new ScheduledJobManager(
                thirdPartySyncJobsConfig,
                schedulerManager,
                scheduledJobRepository,
                scheduledJobStatusRepository,
                scheduledJobTypeRepository,
                repositoryRepository,
                deadlockRetryTemplate));

    Mockito.doReturn(NoOpScheduledJobTest.class)
        .when(scheduledJobManager)
        .loadJobClass(ScheduledJobType.THIRD_PARTY_SYNC.getJobClassName());

    scheduledJobManager.init();
  }

  @Test
  public void testListenersExists() throws Exception {
    assertEquals(
        1, scheduledJobManager.getScheduler().getListenerManager().getJobListeners().size());

    assertEquals(
        1, scheduledJobManager.getScheduler().getListenerManager().getTriggerListeners().size());
  }

  @Test
  public void testQuartzScheduledJob() throws Exception {
    assertEquals(
        2,
        scheduledJobManager
            .getScheduler()
            .getJobKeys(GroupMatcher.groupEquals(ScheduledJobType.THIRD_PARTY_SYNC.toString()))
            .size());

    Thread.sleep(10000);

    ScheduledJob sj =
        scheduledJobRepository.findByUuid("e4c72563-d8f0-4465-9eec-9ed96087665e").get();
    assertEquals(sj.getJobStatus().getEnum(), ScheduledJobStatus.SUCCEEDED);
    assertNotNull(sj.getEndDate());
  }

  //  @Test
  //  public void testQuartzCleanup() throws Exception {
  //    scheduledJobManager.uuidPool = List.of("e4c72563-d8f0-4465-9eec-9ed96087665e");
  //    scheduledJobManager.cleanQuartzJobs();
  //    assertEquals(
  //        1,
  //        scheduledJobManager
  //            .getScheduler()
  //
  // .getJobKeys(GroupMatcher.groupEquals(ScheduledJobType.THIRD_PARTY_SYNC.toString()))
  //            .size());
  //  }
}
