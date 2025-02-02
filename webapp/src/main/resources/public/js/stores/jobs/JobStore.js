import alt from "../../alt";
import JobDataSource from "../../actions/jobs/JobDataSource";
import JobActions from "../../actions/jobs/JobActions";
import RepositoryActions from "../../actions/RepositoryActions";

class JobStore {

    constructor() {
        this.jobs = [];
        this.bindActions(JobActions);
        this.registerAsync(JobDataSource);
    }

    getAllJobs() {
        this.getInstance().getAllJobs();
    }

    getAllJobsSuccess(jobs) {
        this.jobs = jobs;
    }

    triggerJob(job) {
        this.getInstance().triggerJob(job);
    }
}

export default alt.createStore(JobStore, 'JobStore');
