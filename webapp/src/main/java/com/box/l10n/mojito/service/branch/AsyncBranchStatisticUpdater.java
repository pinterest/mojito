package com.box.l10n.mojito.service.branch;

import com.box.l10n.mojito.entity.Branch;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class AsyncBranchStatisticUpdater {

  @Autowired @Lazy BranchStatisticService branchStatisticService;

  public void updateBranchStatistics(
      List<Branch> branches,
      Map<String, ImmutableMap<Long, ForTranslationCountForTmTextUnitId>>
          mapBranchNameToTranslationCountForTextUnitId) {
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    branches.stream()
        .forEach(
            branch ->
                futures.add(
                    updateBranchStatisticsAsync(
                        branch, mapBranchNameToTranslationCountForTextUnitId)));
    // wait for all the updates to complete
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
  }

  @Async("statisticsTaskExecutor")
  public CompletableFuture<Void> updateBranchStatisticsAsync(
      Branch branch,
      Map<String, ImmutableMap<Long, ForTranslationCountForTmTextUnitId>>
          mapBranchNameToTranslationCountForTextUnitId) {
    branchStatisticService.updateBranchStatisticInTx(
        branch,
        mapBranchNameToTranslationCountForTextUnitId.getOrDefault(
            branch.getName(), ImmutableMap.of()));
    return CompletableFuture.completedFuture(null);
  }
}
