package com.box.l10n.mojito.service.repository.statistics;

import com.google.common.collect.Sets;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

/**
 * This class aggregates events that requires repository statistics re-computation and saves at most
 * one record for a repository per second.
 *
 * @author jaurambault
 */
@Component
public class RepositoryStatisticsUpdatedReactor {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(RepositoryStatisticsUpdatedReactor.class);

  RepositoryStatisticsJobScheduler repositoryStatisticsJobScheduler;

  Sinks.Many<Long> sink;

  @Autowired MeterRegistry meterRegistry;

  @Value("${l10n.repositoryStatisticsUpdatedReactor.bufferDuration:PT1S}")
  Duration bufferDuration;

  @Autowired
  public RepositoryStatisticsUpdatedReactor(
      RepositoryStatisticsJobScheduler repositoryStatisticsJobScheduler) {
    this.repositoryStatisticsJobScheduler = repositoryStatisticsJobScheduler;
  }

  @PostConstruct
  public void init() {
    createProcessor();
  }

  void createProcessor() {
    sink = Sinks.many().unicast().onBackpressureBuffer();
    sink.asFlux()
        .buffer(bufferDuration)
        .subscribe(
            repositoryIds -> {
              for (Long repositoryId : Sets.newHashSet(repositoryIds)) {
                try {
                  meterRegistry
                      .counter(
                          "repositoryStatisticsUpdatedReactor.scheduleRepoStatsJob",
                          Tags.of("repositoryId", String.valueOf(repositoryId)))
                      .increment();
                  repositoryStatisticsJobScheduler.schedule(repositoryId);
                } catch (Exception e) {
                  meterRegistry
                      .counter(
                          "repositoryStatisticsUpdatedReactor.scheduleRepoStatsJobException",
                          Tags.of("repositoryId", String.valueOf(repositoryId)))
                      .increment();
                  logger.error(
                      "Failed to schedule repo stats job for repositoryId={}", repositoryId, e);
                }
              }
            });
  }

  /**
   * Generates event that the repository statistics is outdated and needs re-computation.
   *
   * @param repositoryId
   */
  public synchronized void generateEvent(Long repositoryId) {
    sink.emitNext(repositoryId, Sinks.EmitFailureHandler.FAIL_FAST);
  }
}
