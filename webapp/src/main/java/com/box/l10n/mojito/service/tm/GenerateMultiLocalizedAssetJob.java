package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.quartz.QuartzJobInfo;
import com.box.l10n.mojito.quartz.QuartzPollableJob;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.rest.asset.LocaleInfo;
import com.box.l10n.mojito.rest.asset.LocalizedAssetBody;
import com.box.l10n.mojito.rest.asset.MultiLocalizedAssetBody;
import org.springframework.beans.factory.annotation.Autowired;

public class GenerateMultiLocalizedAssetJob
    extends QuartzPollableJob<MultiLocalizedAssetBody, MultiLocalizedAssetBody> {

  @Autowired QuartzPollableTaskScheduler quartzPollableTaskScheduler;

  @Override
  public MultiLocalizedAssetBody call(MultiLocalizedAssetBody input) throws Exception {

    for (LocaleInfo localeInfo : input.getLocaleInfos()) {
      QuartzJobInfo<LocalizedAssetBody, LocalizedAssetBody> quartzJobInfo =
          QuartzJobInfo.newBuilder(GenerateLocalizedAssetJob.class)
              .withInlineInput(false)
              .withParentId(getCurrentPollableTask().getId())
              .withInput(createLocalizedAssetBody(localeInfo, input))
              .withScheduler(input.getSchedulerName())
              .withMessage("Generate localized asset for " + localeInfo.getBcp47Tag())
              .build();
      input.addGenerateLocalizedAddedJobIdToMap(
          localeInfo.getBcp47Tag(),
          quartzPollableTaskScheduler.scheduleJob(quartzJobInfo).getPollableTask().getId());
    }

    return input;
  }

  private LocalizedAssetBody createLocalizedAssetBody(
      LocaleInfo localeInfo, MultiLocalizedAssetBody input) {
    LocalizedAssetBody localizedAssetBody = new LocalizedAssetBody();
    localizedAssetBody.setLocaleId(localeInfo.getLocaleId());
    localizedAssetBody.setContent(input.getSourceContent());
    localizedAssetBody.setAssetId(input.getAssetId());
    localizedAssetBody.setBcp47Tag(localeInfo.getBcp47Tag());
    localizedAssetBody.setContent(input.getSourceContent());
    localizedAssetBody.setFilterConfigIdOverride(input.getFilterConfigIdOverride());
    localizedAssetBody.setFilterOptions(input.getFilterOptions());
    localizedAssetBody.setInheritanceMode(input.getInheritanceMode());
    localizedAssetBody.setPullRunName(input.getPullRunName());
    localizedAssetBody.setStatus(input.getStatus());
    return localizedAssetBody;
  }
}
