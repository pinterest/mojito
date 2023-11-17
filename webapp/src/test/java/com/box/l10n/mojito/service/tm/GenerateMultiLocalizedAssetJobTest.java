package com.box.l10n.mojito.service.tm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.quartz.QuartzJobInfo;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.rest.asset.LocaleInfo;
import com.box.l10n.mojito.rest.asset.LocalizedAssetBody;
import com.box.l10n.mojito.rest.asset.MultiLocalizedAssetBody;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GenerateMultiLocalizedAssetJobTest {

  @Mock QuartzPollableTaskScheduler quartzPollableTaskSchedulerMock;

  @Mock PollableFuture<LocalizedAssetBody> pollableFutureMock;

  @Mock PollableTask pollableTaskMock;

  @Captor ArgumentCaptor<QuartzJobInfo<LocalizedAssetBody, LocalizedAssetBody>> quartzJobInfoCaptor;

  MultiLocalizedAssetBody multiLocalizedAssetBody;

  @Spy
  GenerateMultiLocalizedAssetJob generateMultiLocalizedAssetJob =
      new GenerateMultiLocalizedAssetJob();
;

  @Before
  public void setUp() {
    doReturn(1L).when(generateMultiLocalizedAssetJob).getParentId();
    when(pollableFutureMock.getPollableTask()).thenReturn(pollableTaskMock);
    when(pollableTaskMock.getId()).thenReturn(1L).thenReturn(2L);
    when(quartzPollableTaskSchedulerMock.scheduleJob(isA(QuartzJobInfo.class)))
        .thenReturn(pollableFutureMock);
    generateMultiLocalizedAssetJob.quartzPollableTaskScheduler = quartzPollableTaskSchedulerMock;
    multiLocalizedAssetBody = new MultiLocalizedAssetBody();
    List<LocaleInfo> localeInfos = new ArrayList<>();
    LocaleInfo localeInfo = new LocaleInfo();
    localeInfo.setBcp47Tag("fr-FR");
    localeInfo.setLocaleId(1L);
    LocaleInfo localeInfo2 = new LocaleInfo();
    localeInfo2.setBcp47Tag("ga-IE");
    localeInfo2.setLocaleId(2L);
    localeInfos.add(localeInfo);
    localeInfos.add(localeInfo2);
    multiLocalizedAssetBody.setLocaleInfos(localeInfos);
    multiLocalizedAssetBody.setSourceContent("sourceContent");
    multiLocalizedAssetBody.setAssetId(1L);
    multiLocalizedAssetBody.setSchedulerName("schedulerName");
  }

  @Test
  public void testMultipleLocalisedAssetJobsScheduled() throws Exception {
    MultiLocalizedAssetBody output = generateMultiLocalizedAssetJob.call(multiLocalizedAssetBody);
    verify(quartzPollableTaskSchedulerMock, times(2)).scheduleJob(quartzJobInfoCaptor.capture());
    List<QuartzJobInfo<LocalizedAssetBody, LocalizedAssetBody>> allValues =
        quartzJobInfoCaptor.getAllValues();
    assertThat(allValues.stream().filter(q -> q.getParentId() == 1L).count()).isEqualTo(2);
    assertThat(allValues.stream().map(QuartzJobInfo::getInput))
        .extracting("bcp47Tag")
        .containsExactlyInAnyOrder("fr-FR", "ga-IE");
    assertThat(output.getGenerateLocalizedAssetJobIds().size()).isEqualTo(2);
    assertThat(output.getGenerateLocalizedAssetJobIds().get("fr-FR")).isEqualTo(1L);
    assertThat(output.getGenerateLocalizedAssetJobIds().get("ga-IE")).isEqualTo(2L);
  }
}
