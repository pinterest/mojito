import alt from "../../alt";
import JobDataSource from "../../actions/jobs/JobDataSource";
import JobActions from "../../actions/jobs/JobActions";
import RepositoryActions from "../../actions/RepositoryActions";
import {JobStatus} from "../../utils/JobStatus";

class JobStore {

    constructor() {
        this.jobs = [];
        this.filter = [];
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

    disableJob(job) {
        this.getInstance().disableJob(job);
    }

    enableJob(job) {
        this.getInstance().enableJob(job);
    }

    setJobStatus(args) {
        const [job, status] = args;
        if(status === JobStatus.DISABLED || status === JobStatus.ENABLED) {
            // The jobs enabled attribute should be switched
            this.jobs = this.jobs.map(j => job.id === j.id ? {...j, enabled: status === JobStatus.ENABLED} : j)
        } else {
            this.jobs = this.jobs.map(j => job.id === j.id ? {...j, status} : j)
        }
    }

    setJobFilter(repos) {
        this.filter = repos;
    }
}

export default alt.createStore(JobStore, 'JobStore');
