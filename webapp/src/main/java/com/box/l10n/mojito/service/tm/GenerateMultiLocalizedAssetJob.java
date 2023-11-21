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
      String outputTag =
          localeInfo.getOutputBcp47tag() != null
              ? localeInfo.getOutputBcp47tag()
              : localeInfo.getBcp47Tag();
      QuartzJobInfo<LocalizedAssetBody, LocalizedAssetBody> quartzJobInfo =
          QuartzJobInfo.newBuilder(GenerateLocalizedAssetJob.class)
              .withInlineInput(false)
              .withParentId(getParentId())
              .withInput(createLocalizedAssetBody(localeInfo, input))
              .withScheduler(input.getSchedulerName())
              .withMessage("Generate localized asset for " + outputTag)
              .build();
      input.addGenerateLocalizedAddedJobIdToMap(
          outputTag,
          quartzPollableTaskScheduler.scheduleJob(quartzJobInfo).getPollableTask().getId());
    }

    return input;
  }

  protected long getParentId() {
    return getCurrentPollableTask().getId();
  }

  private LocalizedAssetBody createLocalizedAssetBody(
      LocaleInfo localeInfo, MultiLocalizedAssetBody input) {
    LocalizedAssetBody localizedAssetBody = new LocalizedAssetBody();
    localizedAssetBody.setLocaleId(localeInfo.getLocaleId());
    localizedAssetBody.setContent(input.getSourceContent());
    localizedAssetBody.setAssetId(input.getAssetId());
    localizedAssetBody.setBcp47Tag(localeInfo.getBcp47Tag());
    localizedAssetBody.setOutputBcp47tag(localeInfo.getOutputBcp47tag());
    localizedAssetBody.setContent(input.getSourceContent());
    localizedAssetBody.setFilterConfigIdOverride(input.getFilterConfigIdOverride());
    localizedAssetBody.setFilterOptions(input.getFilterOptions());
    localizedAssetBody.setInheritanceMode(input.getInheritanceMode());
    localizedAssetBody.setPullRunName(input.getPullRunName());
    localizedAssetBody.setStatus(input.getStatus());
    return localizedAssetBody;
  }
}
