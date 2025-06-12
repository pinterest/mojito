package com.box.l10n.mojito.apiclient;

import com.box.l10n.mojito.apiclient.model.ScheduledJobDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ScheduledJobClient {

    @Autowired
    private ScheduledJobWsApi scheduledJobWsApi;

    public void createJob(ScheduledJobDTO scheduledJobDTO) {
        scheduledJobWsApi.createJob(scheduledJobDTO);
    }

    public void updateJob(UUID uuid, ScheduledJobDTO scheduledJobDTO) {
        scheduledJobWsApi.updateJob(scheduledJobDTO, uuid);
    }

    public void deleteJob(UUID uuid) {
        scheduledJobWsApi.deleteJob(uuid);
    }
}
