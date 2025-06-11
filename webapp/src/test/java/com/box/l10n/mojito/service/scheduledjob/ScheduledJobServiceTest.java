package com.box.l10n.mojito.service.scheduledjob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.entity.ScheduledJob;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.repository.RepositoryService;
import org.junit.Before;
import org.junit.Test;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author gerryyang
 */
public class ScheduledJobServiceTest extends ServiceTestBase {

  /** logger */
  static Logger logger = getLogger(ScheduledJobServiceTest.class);

  @Autowired ScheduledJobService scheduledJobService;
  @Autowired RepositoryService repositoryService;
  @Autowired ScheduledJobRepository scheduledJobRepository;
  @Autowired RepositoryRepository repositoryRepository;

  @Before
  public void setup() throws RepositoryNameAlreadyUsedException {
    if (repositoryRepository.findByName("Demo") == null) {
      repositoryService.createRepository("Demo");
    }
    if (repositoryRepository.findByName("Demo1") == null) {
      repositoryService.createRepository("Demo1");
    }
  }

  @Test
  public void testCreateScheduledJobSuccess() throws SchedulerException, ClassNotFoundException {
    ScheduledJobDTO scheduledJobDTO = new ScheduledJobDTO();
    scheduledJobDTO.setRepository("Demo");
    scheduledJobDTO.setCron("0 0/1 * * * ?");
    scheduledJobDTO.setType(ScheduledJobType.THIRD_PARTY_SYNC);
    scheduledJobDTO.setPropertiesString("{\"version\": 1}");

    int initialSize = scheduledJobRepository.findAll().size();
    scheduledJobService.createJob(scheduledJobDTO);
    assertEquals(initialSize + 1, scheduledJobRepository.findAll().size());
  }

  @Test
  public void testCreateScheduledJobFailure() {
    ScheduledJobDTO scheduledJobDTO = new ScheduledJobDTO();
    scheduledJobDTO.setRepository("Invalid Repository");
    scheduledJobDTO.setCron("0 0/1 * * * ?");
    scheduledJobDTO.setType(ScheduledJobType.THIRD_PARTY_SYNC);
    scheduledJobDTO.setPropertiesString("{\"version\": 1}");

    assertThrows(
        ScheduledJobException.class,
        () -> scheduledJobDTO.setPropertiesString("invalid properties string"));
    assertThrows(ScheduledJobException.class, () -> scheduledJobService.createJob(scheduledJobDTO));
  }

  @Test
  public void testUpdateScheduledJobSuccess() throws SchedulerException, ClassNotFoundException {
    ScheduledJobDTO scheduledJobDTO = new ScheduledJobDTO();
    scheduledJobDTO.setRepository("Demo");
    scheduledJobDTO.setCron("0 0/1 * * * ?");
    scheduledJobDTO.setType(ScheduledJobType.THIRD_PARTY_SYNC);
    scheduledJobDTO.setPropertiesString("{\"version\": 1}");
    ScheduledJob createdJob = scheduledJobService.createJob(scheduledJobDTO);

    int initialSize = scheduledJobRepository.findAll().size();

    ScheduledJobDTO updatedJobDTO = new ScheduledJobDTO();
    updatedJobDTO.setRepository("Demo1");
    updatedJobDTO.setCron("0 0/2 * * * ?");
    updatedJobDTO.setType(ScheduledJobType.THIRD_PARTY_SYNC);
    scheduledJobService.updateJob(createdJob.getUuid(), updatedJobDTO);

    assertEquals(initialSize, scheduledJobRepository.findAll().size());
    ScheduledJob updatedJob =
        scheduledJobRepository
            .findByUuid(createdJob.getUuid())
            .orElseThrow(
                () -> new ScheduledJobException("No job found with UUID: " + createdJob.getUuid()));
    assertEquals("0 0/2 * * * ?", updatedJob.getCron());
    assertEquals("Demo1", updatedJob.getRepository().getName());
  }

  @Test
  public void testUpdateScheduledJobFailure() throws SchedulerException, ClassNotFoundException {
    ScheduledJobDTO scheduledJobDTO = new ScheduledJobDTO();
    scheduledJobDTO.setRepository("Demo");
    scheduledJobDTO.setCron("0 0/1 * * * ?");
    scheduledJobDTO.setType(ScheduledJobType.THIRD_PARTY_SYNC);
    scheduledJobDTO.setPropertiesString("{\"version\": 1}");
    ScheduledJob createdJob = scheduledJobService.createJob(scheduledJobDTO);

    ScheduledJobDTO updatedJobDTO = new ScheduledJobDTO();
    updatedJobDTO.setRepository("Invalid Repository");
    updatedJobDTO.setCron("0 0/2 * * * ?");
    updatedJobDTO.setType(ScheduledJobType.THIRD_PARTY_SYNC);

    assertThrows(
        ScheduledJobException.class,
        () -> updatedJobDTO.setPropertiesString("invalid properties string"));
    assertThrows(
        ScheduledJobException.class,
        () -> scheduledJobService.updateJob(createdJob.getUuid(), updatedJobDTO));
  }

  @Test
  public void testDeleteScheduledJob() throws SchedulerException, ClassNotFoundException {
    ScheduledJobDTO scheduledJobDTO = new ScheduledJobDTO();
    scheduledJobDTO.setRepository("Demo");
    scheduledJobDTO.setCron("0 0/1 * * * ?");
    scheduledJobDTO.setType(ScheduledJobType.THIRD_PARTY_SYNC);
    scheduledJobDTO.setPropertiesString("{\"version\": 1}");
    ScheduledJob createdJob = scheduledJobService.createJob(scheduledJobDTO);

    int initialSize = scheduledJobRepository.findAll().size();
    scheduledJobService.deleteJob(createdJob);
    assertEquals(initialSize - 1, scheduledJobRepository.findAll().size());
  }
}
