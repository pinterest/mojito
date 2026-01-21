package com.box.l10n.mojito.service.branch;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BranchTextUnitStatusDTO {

  private final Long branchId;
  private final String repositoryName;
  private final String branchName;

  private final Map<String, List<TextUnitStatusDTO>> localeTextUnitStatus;

  public BranchTextUnitStatusDTO(
      List<BranchTextUnitStatusDataModel> branchTextUnitStatusDataModels) {
    if (branchTextUnitStatusDataModels == null || branchTextUnitStatusDataModels.isEmpty()) {
      throw new IllegalArgumentException("BranchTextUnitStatusDataModels is null or empty");
    }

    this.branchName = branchTextUnitStatusDataModels.getFirst().getBranchName();
    this.repositoryName = branchTextUnitStatusDataModels.getFirst().getRepositoryName();
    this.branchId = branchTextUnitStatusDataModels.getFirst().getBranchId();

    this.localeTextUnitStatus =
        branchTextUnitStatusDataModels.stream()
            .filter(tu -> tu.getBcp47Tag() != null)
            .collect(
                Collectors.groupingBy(
                    BranchTextUnitStatusDataModel::getBcp47Tag,
                    Collectors.mapping(
                        textUnitStatusDataModel ->
                            new TextUnitStatusDTO(
                                textUnitStatusDataModel.getTextUnitId(),
                                textUnitStatusDataModel.getVariantId(),
                                textUnitStatusDataModel.getCurrentVariantId(),
                                textUnitStatusDataModel.getCreatedDate(),
                                textUnitStatusDataModel.getStatus(),
                                textUnitStatusDataModel.getContent(),
                                textUnitStatusDataModel.getComment()),
                        Collectors.toList())));
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public String getBranchName() {
    return branchName;
  }

  public Long getBranchId() {
    return branchId;
  }

  public Map<String, List<TextUnitStatusDTO>> getLocaleTextUnitStatus() {
    return localeTextUnitStatus;
  }
}
