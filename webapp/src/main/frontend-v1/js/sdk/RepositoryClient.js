import BaseClient from "./BaseClient.js";

class RepositoryClient extends BaseClient {
    createRepository(repository) {
        return this.post(this.getUrl(), repository);
    }

    updateRepository(repository) {
        return this.patch(this.getUrl() + "/" + repository.id, repository);
    }

    getRepositories() {
        return this.get(this.getUrl(), {});
    }

    getEntityName() {
        return 'repositories';
    }
};

export default new RepositoryClient();