package com.box.l10n.mojito.service.assetExtraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.localtm.merger.Branch;
import com.box.l10n.mojito.localtm.merger.BranchData;
import com.box.l10n.mojito.localtm.merger.BranchStateTextUnit;
import com.box.l10n.mojito.localtm.merger.MultiBranchState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

public class MultiBranchStateBlobStorageTest extends ServiceTestBase {

  static Logger logger = getLogger(MultiBranchStateBlobStorageTest.class);

  @Autowired MultiBranchStateBlobStorage multiBranchStateBlobStorage;

  @Test
  public void test() {
    long assetExtractionId = 123456789123L;
    long version = 1L;

    multiBranchStateBlobStorage.deleteMultiBranchStateForAssetExtractionId(
        assetExtractionId, version);
    Optional<MultiBranchState> multiBranchStateForAssetExtractionId =
        multiBranchStateBlobStorage.getMultiBranchStateForAssetExtractionId(
            assetExtractionId, version);
    Assertions.assertFalse(multiBranchStateForAssetExtractionId.isPresent());

    // TODO(jean) 2-JSR310 loss of precision here, supposedly  due to JSON serialization/deserialization
    // Must understand better the implication of those... ser/des could support nano second for some use case
    // but for the API layer we need to keep backward compatibility
    Branch branchTest =
        Branch.builder().name("test").createdAt(JSR310Migration.newDateTimeEmptyCtor().withNano(0)).build();
    MultiBranchState multiBranchState =
        MultiBranchState.builder()
            .branches(ImmutableSet.of(branchTest))
            .branchStateTextUnits(
                ImmutableList.of(
                    BranchStateTextUnit.builder()
                        .tmTextUnitId(123456L)
                        .name("name")
                        .branchNameToBranchDatas(
                            ImmutableMap.of(
                                branchTest.getName(),
                                BranchData.of().withUsages(ImmutableSet.of("somefile"))))
                        .build()))
            .build();

    multiBranchStateBlobStorage.putMultiBranchStateForAssetExtractionId(
        multiBranchState, assetExtractionId, version);

    multiBranchStateForAssetExtractionId =
        multiBranchStateBlobStorage.getMultiBranchStateForAssetExtractionId(
            assetExtractionId, version);
    assertThat(multiBranchStateForAssetExtractionId.get())
        .isEqualToComparingFieldByField(multiBranchState);
  }
}
