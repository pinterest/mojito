import BranchTextUnitsPaginatorStore from "../../stores/branches/BranchTextUnitsPaginatorStore.js";
import BranchTextUnitParameters from "../../sdk/BranchTextUnitParameters.js";
import BranchTextUnitsParamStore from "../../stores/branches/BranchTextUnitsParamStore.js";
import BranchTextUnitClient from "../../sdk/BranchTextUnitClient.js";
import BranchTextUnitsPageActions from "./BranchTextUnitsPageActions.js";

const BranchTextUnitsDataSource = {
    performBranchTextUnitsSearch: {
        remote() {
            const { branchStatisticId } = BranchTextUnitsParamStore.getState();
            if (!branchStatisticId) {
                return new Promise(function (resolve) {
                    resolve({
                        "hasNext": false,
                        "size": 10,
                        "content": [],
                        "hasPrevious": false,
                        "number": 0,
                        "first": true,
                        "numberOfElements": 0,
                        "totalPages": 1,
                        "totalElements": 0,
                        "last": true
                    });
                });
            }
            const branchTextUnitsPaginatorState = BranchTextUnitsPaginatorStore.getState();
            const branchTextUnitParameters = new BranchTextUnitParameters();
            branchTextUnitParameters.page(branchTextUnitsPaginatorState.currentPageNumber - 1);
            branchTextUnitParameters.size(branchTextUnitsPaginatorState.limit);
            return BranchTextUnitClient.getBranchTextUnits(branchStatisticId, branchTextUnitParameters);
        },
        success: BranchTextUnitsPageActions.getBranchTextUnitsSuccess,
        error: BranchTextUnitsPageActions.getBranchTextUnitsError
    }
};

export default BranchTextUnitsDataSource;
