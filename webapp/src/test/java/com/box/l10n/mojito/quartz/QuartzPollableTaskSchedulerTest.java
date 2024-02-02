package com.box.l10n.mojito.quartz;

import static com.box.l10n.mojito.quartz.QuartzSchedulerManager.DEFAULT_SCHEDULER_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableTaskBlobStorage;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.quartz.DisallowConcurrentExecution;
import org.springframework.beans.factory.annotation.Autowired;

public class QuartzPollableTaskSchedulerTest extends ServiceTestBase {

  @Autowired QuartzPollableTaskScheduler quartzPollableTaskScheduler;

  @Autowired PollableTaskBlobStorage pollableTaskBlobStorage;

  @Test
  public void test() throws ExecutionException, InterruptedException {
    PollableFuture<AQuartzPollableJobOutput> pollableFuture =
        quartzPollableTaskScheduler.scheduleJob(
            AQuartzPollableJob.class, 10L, DEFAULT_SCHEDULER_NAME);
    AQuartzPollableJobOutput s = pollableFuture.get();
    assertEquals("output: 10", s.getOutput());

    PollableFuture<AQuartzPollableJobOutput> stringPollableFuture2 =
        quartzPollableTaskScheduler.scheduleJob(
            AQuartzPollableJob.class, 10L, DEFAULT_SCHEDULER_NAME);
    AQuartzPollableJobOutput s2 = stringPollableFuture2.get();
    assertEquals("output: 10", s2.getOutput());
  }

  @Test
  public void testVoid() throws ExecutionException, InterruptedException {
    PollableFuture<Void> pollableFuture =
        quartzPollableTaskScheduler.scheduleJob(
            VoidQuartzPollableJob.class, 10L, DEFAULT_SCHEDULER_NAME);
    Void aVoid = pollableFuture.get();
    assertEquals(null, aVoid);
    try {
      Object output =
          pollableTaskBlobStorage.getOutputJson(pollableFuture.getPollableTask().getId());
      fail();
    } catch (RuntimeException re) {
      assertTrue(re.getMessage().startsWith("Can't get the output json for:"));
    }
  }

  @Test
  public void testGetShortClassName() {
    assertEquals(
        "com.box.l10n.mojito.quartz.QuartzPollableTaskSchedulerTest",
        quartzPollableTaskScheduler.getShortClassName(QuartzPollableTaskSchedulerTest.class));
    assertEquals(
        "c.b.l.m.q.Q.ALongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongNameClassForTest",
        quartzPollableTaskScheduler.getShortClassName(
            ALongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongNameClassForTest
                .class));
  }

  @Test
  public void testRescheduleNonConcurrentJobs() throws ExecutionException, InterruptedException {
    /**
     * This test verifies that in the event of a rescheduling of a job that disallows concurrent
     * execution, the pollable task associated with the Quartz Trigger in a BLOCKED state is
     * returned for multiple calls to scheduleJob with the same uniqueId set.
     *
     * <p>This ensures that no more than 2 pollable tasks (the executing tasks and the task waiting
     * to execute) are present at the same time for the same uniqueId.
     *
     * <p>The tests steps are as follows: 1. Schedule a job with a uniqueId 2. Schedule a job with
     * the same uniqueId, this time with different output 3. Schedule a job with the same uniqueId,
     * this time with the same output as the first job 4. Verify that the output of the first job is
     * returned as expected 5. Verify that the output of the second job is returned as expected 6.
     * Verify that the output of the third job is returned however its output is the same as the
     * second job, as the associated pollable task for job 2 was returned as it was in a BLOCKED
     * state due to job 1 executing.
     */
    QuartzJobInfo<Long, AQuartzPollableJobOutput> quartzJobInfo =
        QuartzJobInfo.newBuilder(AQuartzPollableJob3.class)
            .withUniqueId("test1")
            .withInput(10L)
            .build();
    QuartzJobInfo<Long, AQuartzPollableJobOutput> quartzJobInfo2 =
        QuartzJobInfo.newBuilder(AQuartzPollableJob3.class)
            .withUniqueId("test1")
            .withInput(20L)
            .build();
    QuartzJobInfo<Long, AQuartzPollableJobOutput> quartzJobInfo3 =
        QuartzJobInfo.newBuilder(AQuartzPollableJob3.class)
            .withUniqueId("test1")
            .withInput(10L)
            .build();
    PollableFuture<AQuartzPollableJobOutput> pollableFuture =
        quartzPollableTaskScheduler.scheduleJob(quartzJobInfo);
    PollableFuture<AQuartzPollableJobOutput> pollableFuture2 =
        quartzPollableTaskScheduler.scheduleJob(quartzJobInfo2);
    PollableFuture<AQuartzPollableJobOutput> pollableFuture3 =
        quartzPollableTaskScheduler.scheduleJob(quartzJobInfo3);
    AQuartzPollableJobOutput s = pollableFuture.get();
    assertEquals("output: 10", s.getOutput());
    AQuartzPollableJobOutput s2 = pollableFuture2.get();
    assertEquals("output: 20", s2.getOutput());
    AQuartzPollableJobOutput s3 = pollableFuture3.get();
    assertEquals("output: 20", s3.getOutput());
  }

  @DisallowConcurrentExecution
  public static class AQuartzPollableJob3 extends AQuartzPollableJob2 {
    @Override
    public AQuartzPollableJobOutput call(Long input) throws Exception {
      Thread.sleep(5000);
      return super.call(input);
    }
  }

  public static class AQuartzPollableJob extends AQuartzPollableJob2 {}

  public static class AQuartzPollableJob2
      extends QuartzPollableJob<Long, AQuartzPollableJobOutput> {
    @Override
    public AQuartzPollableJobOutput call(Long input) throws Exception {
      AQuartzPollableJobOutput aQuartzPollableJobOutput = new AQuartzPollableJobOutput();
      aQuartzPollableJobOutput.setOutput("output: " + input);
      return aQuartzPollableJobOutput;
    }
  }

  static class AQuartzPollableJobOutput {
    String output;

    public String getOutput() {
      return output;
    }

    public void setOutput(String output) {
      this.output = output;
    }
  }

  public static class VoidQuartzPollableJob extends QuartzPollableJob<Long, Void> {
    @Override
    public Void call(Long input) throws Exception {
      return null;
    }
  }

  static
  class ALongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongNameClassForTest {}
}
