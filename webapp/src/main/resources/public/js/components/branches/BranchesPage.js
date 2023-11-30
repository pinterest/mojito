import React from "react";
import {withRouter} from "react-router";
import AltContainer from "alt-container";

import BranchesAddScreenshotButton from ".//BranchesAddScreenshotButton";
import BranchesStore from "../../stores/branches/BranchesStore";
import BranchesSearchText from "./BranchesSearchText";
import BranchesStatusDropdown from "./BranchesStatusDropdown";
import BranchesSearchParamStore from "../../stores/branches/BranchesSearchParamStore";
import BranchesPageActions from "../../actions/branches/BranchesPageActions";
import BranchesSearchParamsActions from "../../actions/branches/BranchesSearchParamsActions";
import BranchesSearchResults from "./BranchesSearchResults";
import BranchesScreenshotUploadModal from "./BranchesScreenshotUploadModal";
import BranchesScreenshotUploadModalStore from "../../stores/branches/BranchesScreenshotUploadModalStore";
import BranchesScreenshotUploadActions from "../../actions/branches/BranchesScreenshotUploadActions";
import ScreenshotViewerModal from "../screenshots/ScreenshotViewerModal";
import BranchesScreenshotViewerStore from "../../stores/branches/BranchesScreenshotViewerStore";
import BranchesScreenshotViewerActions from "../../actions/branches/BranchesScreenshotViewerActions";
import BranchesPaginatorStore from "../../stores/branches/BranchesPaginatorStore";
import BranchesPaginatorActions from "../../actions/branches/BranchesPaginatorActions";
import BranchesHistoryActions from "../../actions/branches/BranchesHistoryActions";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";
import Paginator from "../widgets/Paginator";
import RepositoryStore from "../../stores/RepositoryStore";
import SearchParamsStore from "../../stores/workbench/SearchParamsStore";
import SearchConstants from "../../utils/SearchConstants";


class BranchesPage extends React.Component {

    render() {

        return (
            <div>
                <AltContainer stores={
                    {
                        disabled: function (props) {
                            return {
                                store: BranchesStore,
                                value: BranchesStore.getState().selectedBranchTextUnitIds.length === 0
                            };
                        }
                    }}>
                    <BranchesAddScreenshotButton
                        onClick={() => {
                            BranchesScreenshotUploadActions.openWithBranch();
                        }}
                    />
                </AltContainer>

                <AltContainer store={BranchesSearchParamStore}>
                    <BranchesSearchText
                        onBranchesSearchTextChanged={
                            (text) => {
                                BranchesHistoryActions.disableHistoryUpdate();
                                BranchesSearchParamsActions.changeSearchText(text);
                            }
                        }

                        onPerformSearch={() => {
                            BranchesHistoryActions.disableHistoryUpdate();
                            BranchesPaginatorActions.changeCurrentPageNumber(1);
                            BranchesSearchParamsActions.changeOnlyMyBranches(false);
                            BranchesSearchParamsActions.changeDeleted(true);
                            BranchesSearchParamsActions.changeUndeleted(true);
                            BranchesSearchParamsActions.changeEmpty(true);
                            BranchesSearchParamsActions.changeNotEmpty(true);
                            BranchesHistoryActions.enableHistoryUpdate();
                            BranchesPageActions.getBranches();
                        }}
                    />
                </AltContainer>

                <AltContainer store={BranchesSearchParamStore}>
                    <BranchesStatusDropdown
                        onDeletedChanged={(deleted) => {
                            BranchesHistoryActions.disableHistoryUpdate();
                            BranchesSearchParamsActions.changeDeleted(deleted);
                            BranchesPageActions.changeSelectedBranchTextUnitIds([]);
                            BranchesHistoryActions.enableHistoryUpdate();
                            BranchesPageActions.getBranches();
                        }}

                        onUndeletedChanged={(undeleted) => {
                            BranchesHistoryActions.disableHistoryUpdate();
                            BranchesSearchParamsActions.changeUndeleted(undeleted);
                            BranchesPageActions.changeSelectedBranchTextUnitIds([]);
                            BranchesHistoryActions.enableHistoryUpdate();
                            BranchesPageActions.getBranches();
                        }}

                        onEmptyChanged={(empty) => {
                            BranchesHistoryActions.disableHistoryUpdate();
                            BranchesSearchParamsActions.changeEmpty(empty);
                            BranchesPageActions.changeSelectedBranchTextUnitIds([]);
                            BranchesHistoryActions.enableHistoryUpdate();
                            BranchesPageActions.getBranches();
                        }}

                        onNotEmptyChanged={(notEmpty) => {
                            BranchesHistoryActions.disableHistoryUpdate();
                            BranchesSearchParamsActions.changeNotEmpty(notEmpty);
                            BranchesPageActions.changeSelectedBranchTextUnitIds([]);
                            BranchesHistoryActions.enableHistoryUpdate();
                            BranchesPageActions.getBranches();
                        }}

                        onOnlyMyBranchesChanged={(onlyMyBranches) => {
                            BranchesHistoryActions.disableHistoryUpdate();
                            BranchesSearchParamsActions.changeOnlyMyBranches(onlyMyBranches);
                            BranchesPageActions.changeSelectedBranchTextUnitIds([]);
                            BranchesHistoryActions.enableHistoryUpdate();
                            BranchesPageActions.getBranches();
                        }}

                        onCreatedBeforeChanged={(createdBefore) => {
                            BranchesHistoryActions.disableHistoryUpdate();
                            BranchesSearchParamsActions.changeCreatedBefore(createdBefore)
                            BranchesPageActions.changeSelectedBranchTextUnitIds([]);
                            BranchesHistoryActions.enableHistoryUpdate();
                            BranchesPageActions.getBranches();
                        }}

                        onCreatedAfterChanged={(createdAfter) => {
                            BranchesHistoryActions.disableHistoryUpdate();
                            BranchesSearchParamsActions.changeCreatedAfter(createdAfter)
                            BranchesPageActions.changeSelectedBranchTextUnitIds([]);
                            BranchesHistoryActions.enableHistoryUpdate();
                            BranchesPageActions.getBranches();
                        }}
                    />
                </AltContainer>

                <AltContainer store={BranchesPaginatorStore}>
                    <Paginator
                        onPreviousPageClicked={() => {
                            BranchesHistoryActions.disableHistoryUpdate();
                            BranchesPaginatorActions.goToPreviousPage();
                            BranchesPageActions.changeSelectedBranchTextUnitIds([]);
                            BranchesHistoryActions.enableHistoryUpdate();
                            BranchesPageActions.getBranches();
                        }}
                        onNextPageClicked={() => {
                            BranchesHistoryActions.disableHistoryUpdate();
                            BranchesPaginatorActions.goToNextPage();
                            BranchesPageActions.changeSelectedBranchTextUnitIds([]);
                            BranchesHistoryActions.enableHistoryUpdate();
                            BranchesPageActions.getBranches();
                        }}/>
                </AltContainer>

                <AltContainer store={BranchesStore}>
                    <BranchesSearchResults
                        onChangeSelectedBranchTextUnits={(selectedBranchTextUnitIds) => {
                            BranchesPageActions.changeSelectedBranchTextUnitIds(selectedBranchTextUnitIds);
                        }}

                        onChangeOpenBranchStatistic={(branchStatisticId) => {
                            BranchesPageActions.changeOpenBranchStatistic(branchStatisticId);
                            BranchesPageActions.changeSelectedBranchTextUnitIds([]);
                        }}

                        onShowBranchScreenshotsClick={(branchStatisticId) => {
                            BranchesPageActions.changeOpenBranchStatistic(branchStatisticId);
                            BranchesScreenshotViewerActions.openScreenshotsViewer(branchStatisticId);
                        }}

                        onNeedTranslationClick={(branchStatistic, tmTextUnitId, forTranslation) => {

                            let repoIds = [branchStatistic.branch.repository.id];

                            let params = {
                                "changedParam": SearchConstants.UPDATE_ALL,
                                "repoIds": repoIds,
                                "branchId": branchStatistic.branch.id,
                                "bcp47Tags": RepositoryStore.getAllBcp47TagsForRepositoryIds(repoIds, true),
                                "status": forTranslation ? SearchParamsStore.STATUS.FOR_TRANSLATION : SearchParamsStore.STATUS.ALL,
                            }

                            if (tmTextUnitId != null) {
                                params["tmTextUnitIds"] = [tmTextUnitId];
                            }

                            WorkbenchActions.searchParamsChanged(params);
                            this.props.router.push("/workbench", null, null);
                        }}

                        onTextUnitNameClick={(branchStatistic, tmTextUnitId) => {

                            let repoIds = [branchStatistic.branch.repository.id];

                            let params = {
                                "changedParam": SearchConstants.UPDATE_ALL,
                                "repoIds": repoIds,
                                "tmTextUnitIds": [tmTextUnitId],
                                "bcp47Tags": RepositoryStore.getAllBcp47TagsForRepositoryIds(repoIds, true),
                                "status": SearchParamsStore.STATUS.ALL,
                            }

                            WorkbenchActions.searchParamsChanged(params);
                            this.props.router.push("/workbench", null, null);
                        }}
                    />
                </AltContainer>

                <AltContainer store={BranchesScreenshotUploadModalStore}>
                    <BranchesScreenshotUploadModal
                        onCancel={() => {
                            BranchesScreenshotUploadActions.close();
                        }}

                        onSelectedFileChange={(files) => {
                            BranchesScreenshotUploadActions.changeSelectedFiles(files);
                        }}

                        onUpload={() => {
                            BranchesScreenshotUploadActions.uploadScreenshotImage();
                        }}
                    />
                </AltContainer>

                <AltContainer store={BranchesScreenshotViewerStore}>
                    <ScreenshotViewerModal
                        onGoToPrevious={() => {
                            BranchesScreenshotViewerActions.goToPrevious();
                        }}
                        onGoToNext={() => {
                            BranchesScreenshotViewerActions.goToNext();
                        }}
                        onClose={() => {
                            BranchesScreenshotViewerActions.closeScreenshotsViewer();
                        }}
                        onDelete={() => {
                            BranchesScreenshotViewerActions.delete();
                        }}
                    />
                </AltContainer>

            </div>
        );
    }
};

export default withRouter(BranchesPage);
