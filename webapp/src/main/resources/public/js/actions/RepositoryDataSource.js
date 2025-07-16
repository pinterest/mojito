import RepositoryClient from "../sdk/RepositoryClient.js";
import RepositoryActions from "./RepositoryActions.js";

const RepositoryDataSource = {
    getAllRepositories: {
        remote() {
            return RepositoryClient.getRepositories();
        },

        success: RepositoryActions.getAllRepositoriesSuccess,
        error: RepositoryActions.getAllRepositoriesError
    }
};

export default RepositoryDataSource;
