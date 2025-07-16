import alt from "../alt.js";

class RepositoryActions {
    constructor() {
        this.generateActions(
            "getAllRepositories",
            "getAllRepositoriesSuccess",
            "getAllRepositoriesError"
        );
    }
}

export default alt.createActions(RepositoryActions);
