import alt from "../...js";

class BranchTextUnitsPageActions {
    constructor() {
        this.generateActions(
            "getBranchTextUnits",
            "resetBranchTextUnitsSearchParams",
            "getBranchTextUnitsSuccess",
            "getBranchTextUnitsError",
        );
    }
}

export default alt.createActions(BranchTextUnitsPageActions);
