package com.box.l10n.mojito.service.ai;

import java.time.Period;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Service;

@Service
public class AIStringCheckCleanupService {
  private final AIStringCheckRepository aiStringCheckRepository;

  public AIStringCheckCleanupService(AIStringCheckRepository aiStringCheckRepository) {
    this.aiStringCheckRepository = aiStringCheckRepository;
  }

  public void cleanup(Period period) {
    ZonedDateTime beforeDate = ZonedDateTime.now().minus(period);
    this.aiStringCheckRepository.deleteAllOlderThan(beforeDate);
  }
}
