import alt from "../../alt.js";
import BranchesDataSource from "../../actions/branches/BranchesHistoryActions.js";
import BranchesPaginatorStore from "../../stores/branches/BranchesPaginatorStore.js";
import BranchesPageActions from "../../actions/branches/BranchesPageActions.js";
import BranchesPaginatorActions from "../../actions/branches/BranchesPaginatorActions.js";
import BranchesSearchParamsActions from "../../actions/branches/BranchesSearchParamsActions.js";

class BranchesHistoryStore {

    constructor() {
        this.setDefaultState();
        this.bindActions(BranchesDataSource);
        this.bindActions(BranchesPageActions);
        this.bindActions(BranchesPaginatorActions);
        this.bindActions(BranchesSearchParamsActions);
        ;
    }

    setDefaultState() {
        // use to skip location update (Initiated by the BranchesPage) when
        // this store is initializing from the browser location or to do
        // group updates together
        this.skipLocationHistoryUpdate = false;

        this.openBranchStatistic = null;
        // this.selectedBranchTextUnitIds = [];
        this.currentPageNumber = 1;
        this.searchText = "";
        this.deleted = false;
        this.undeleted = true;
        this.empty = true;
        this.notEmpty = false;
        this.onlyMyBranches = true;
    }

    enableHistoryUpdate() {
        this.skipLocationHistoryUpdate = false;
    }

    disableHistoryUpdate() {
        this.skipLocationHistoryUpdate = true;
    }

    changeOpenBranchStatistic(openBranchStatistic) {
        this.openBranchStatistic = openBranchStatistic;
    }

    changeCreatedBefore(createdBefore) {
        this.createdBefore = createdBefore;
    }

    changeCreatedAfter(createdAfter) {
        this.createdAfter = createdAfter;
    }

    changeSearchText(searchText) {
        this.searchText = searchText;
    }

    changeDeleted(deleted) {
        this.deleted = deleted;
    }

    changeUndeleted(undeleted) {
        this.undeleted = undeleted;
    }

    changeEmpty(empty) {
        this.empty = empty;
    }

    changeNotEmpty(notEmpty) {
        this.notEmpty = notEmpty;
    }

    changeOnlyMyBranches(onlyMyBranches) {
        this.onlyMyBranches = onlyMyBranches;
    }

    goToNextPage() {
        this.currentPageNumber = BranchesPaginatorStore.getState().currentPageNumber;
    }

    goToPreviousPage() {
        this.currentPageNumber = BranchesPaginatorStore.getState().currentPageNumber;
    }

    changeCurrentPageNumber(currentPageNumber) {
        this.currentPageNumber = currentPageNumber;
    }

    static getQueryParams() {
        const params = this.getState();
        delete params.skipLocationHistoryUpdate;
        return params;
    }

    static initStoreFromLocationQuery(query) {
        let { searchText } = query;
        const {
            openBranchStatistic, currentPageNumber = 1,
            deleted = "false", undeleted = "true", empty = "true", notEmpty = "true", onlyMyBranches = "true",
            createdBefore = null, createdAfter = null
        } = query;

        let selectedBranchTextUnitIds = query["selectedBranchTextUnitIds[]"];

        BranchesDataSource.disableHistoryUpdate();

        if (selectedBranchTextUnitIds) {
            if (!Array.isArray(selectedBranchTextUnitIds)) {
                selectedBranchTextUnitIds = [parseInt(selectedBranchTextUnitIds)];
            } else {
                selectedBranchTextUnitIds = selectedBranchTextUnitIds.map((v) => parseInt(v));
            }
        } else {
            selectedBranchTextUnitIds = [];
        }
        BranchesPageActions.changeSelectedBranchTextUnitIds(selectedBranchTextUnitIds);

        if (!searchText) {
            searchText = "";
        }

        BranchesPaginatorActions.changeCurrentPageNumber(parseInt(currentPageNumber));
        BranchesSearchParamsActions.changeSearchText(searchText);
        BranchesSearchParamsActions.changeDeleted(deleted === "true");
        BranchesSearchParamsActions.changeUndeleted(undeleted === "true");
        BranchesSearchParamsActions.changeEmpty(empty === "true");
        BranchesSearchParamsActions.changeNotEmpty(notEmpty === "true");
        BranchesSearchParamsActions.changeOnlyMyBranches(onlyMyBranches === "true");
        if (createdBefore !== null) {
            BranchesSearchParamsActions.changeCreatedBefore(createdBefore);
        }
        if (createdAfter !== null) {
            BranchesSearchParamsActions.changeCreatedAfter(createdAfter);
        }

        if (openBranchStatistic) {
            BranchesPageActions.changeOpenBranchStatistic(parseInt(openBranchStatistic));
        }

        BranchesDataSource.enableHistoryUpdate();

        BranchesPageActions.getBranches();
    }
}

export default alt.createStore(BranchesHistoryStore, 'BranchesHistoryStore');
