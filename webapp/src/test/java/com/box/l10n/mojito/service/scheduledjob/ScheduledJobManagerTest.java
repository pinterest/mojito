// package com.box.l10n.mojito.service.scheduledjob;
//
// import static org.junit.Assert.assertEquals;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.Mockito.doNothing;
//
// import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
// import org.junit.Before;
// import org.junit.Test;
// import org.mockito.Mock;
// import org.mockito.MockitoAnnotations;
// import org.quartz.JobExecutionContext;
// import org.quartz.JobExecutionException;
// import org.quartz.impl.matchers.GroupMatcher;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.test.context.ContextConfiguration;
// import org.springframework.test.context.TestPropertySource;
//
// @TestPropertySource(
//    properties = {
//      "l10n.scheduledJobs.enabled=true",
//      "l10n.thirdPartySyncJobs.test1.uuid=e4c72563-d8f0-4465-9eec-9ed96087665e",
//      "l10n.thirdPartySyncJobs.test1.cron=0/2 * * * * ?",
//      "l10n.thirdPartySyncJobs.test1.repository=scheduled-job-1",
//      "l10n.thirdPartySyncJobs.test1.thirdPartyProjectId=123456",
//      "l10n.thirdPartySyncJobs.test1.actions=PUSH,PULL,MAP_TEXTUNIT,PUSH_SCREENSHOT",
//      "",
//      "l10n.thirdPartySyncJobs.test2.uuid=e4c72563-d8f0-4465-9eec-aaa96087665e",
//      "l10n.thirdPartySyncJobs.test2.cron=0 0/5 * * * ?",
//      "l10n.thirdPartySyncJobs.test2.repository=scheduled-job-2",
//      "l10n.thirdPartySyncJobs.test2.thirdPartyProjectId=123456",
//      "l10n.thirdPartySyncJobs.test2.actions=PUSH,PULL,MAP_TEXTUNIT,PUSH_SCREENSHOT",
//      "",
//      "l10n.thirdPartySyncJobs.test3.cron=0 0/5 * * * ?",
//      "l10n.thirdPartySyncJobs.test3.repository=scheduled-job-3",
//      "l10n.thirdPartySyncJobs.test3.thirdPartyProjectId=123456",
//      "l10n.thirdPartySyncJobs.test3.actions=PUSH,PULL,MAP_TEXTUNIT,PUSH_SCREENSHOT",
//      "",
//      "l10n.thirdPartySyncJobs.test4.uuid=e4c72563-d8f0-4465-9eec-aaa96087665e",
//      "l10n.thirdPartySyncJobs.test4.repository=scheduled-job-4",
//      "l10n.thirdPartySyncJobs.test4.thirdPartyProjectId=123456",
//      "l10n.thirdPartySyncJobs.test4.actions=PUSH,PULL,MAP_TEXTUNIT,PUSH_SCREENSHOT"
//    })
// @ContextConfiguration(classes = {ScheduledJobTestConfiguration.class})
// public class ScheduledJobManagerTest extends ServiceTestBase {
//  @Mock IScheduledJob scheduledThirdPartySync;
//  @Autowired ScheduledJobManager scheduledJobManager;
//
//  @Before
//  public void setUp() throws JobExecutionException {
//    MockitoAnnotations.openMocks(this);
//    doNothing().when(scheduledThirdPartySync).execute(any(JobExecutionContext.class));
//  }
//
//  @Test
//  public void testListenersExists() throws Exception {
//    assertEquals(
//        1, scheduledJobManager.getScheduler().getListenerManager().getJobListeners().size());
//
//    assertEquals(
//        1, scheduledJobManager.getScheduler().getListenerManager().getTriggerListeners().size());
//  }
//
//  @Test
//  public void testQuartzScheduledJob() throws Exception {
//    assertEquals(
//        2,
//        scheduledJobManager
//            .getScheduler()
//            .getJobKeys(GroupMatcher.groupEquals(ScheduledJobType.THIRD_PARTY_SYNC.toString()))
//            .size());
//
//    Thread.sleep(10000);
//  }
//
//  //  @Test
//  //  public void testQuartzCleanup() throws Exception {
//  //    scheduledJobManager.uuidPool = List.of("e4c72563-d8f0-4465-9eec-9ed96087665e");
//  //    scheduledJobManager.cleanQuartzJobs();
//  //    assertEquals(
//  //        1,
//  //        scheduledJobManager
//  //            .getScheduler()
//  //
// .getJobKeys(GroupMatcher.groupEquals(ScheduledJobType.THIRD_PARTY_SYNC.toString()))
//  //            .size());
//  //  }
// }
