import alt from "../../alt";
import JobDataSource from "../../actions/jobs/JobDataSource";
import JobActions from "../../actions/jobs/JobActions";
import { JobStatus } from "../../utils/JobStatus";

class JobStore {

    constructor() {
        this.jobs = [];
        this.filter = [];
        this.errorMessage = null;
        this.bindActions(JobActions);
        this.registerAsync(JobDataSource);
    }

    createJob(job) {
        this.getInstance().createJob(job);
    }

    createJobSuccess(job) {
        this.jobs = [...this.jobs, job];
    }

    createJobError(error) {
        if (error.response) {
            error.response.json().then(data => {
                this.setErrorMessage(data.message);
            });
        }
    }

    updateJob(job) {
        this.getInstance().updateJob(job);
    }

    updateJobSuccess(job) {
        this.jobs = this.jobs.map(j => j.id === job.id ? { ...j, ...job } : j);
    }

    updateJobError(error) {
        if (error.response) {
            error.response.json().then(data => {
                this.setErrorMessage(data.message);
            });
        }
    }

    deleteJob(job) {
        this.getInstance().deleteJob(job);
    }

    deleteJobSuccess() {
        this.getAllJobs();
    }

    restoreJob(job) {
        this.getInstance().restoreJob(job);
    }

    restoreJobSuccess() {
        this.getAllJobs();
    }

    setErrorMessage(message) {
        this.errorMessage = message;
    }

    static getErrorMessage() {
        return this.getState().errorMessage;
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

    triggerJobSuccess(response) {
        const jobId = response.jobId;
        // Update job on client side before the full sync (poll) occurs
        this.jobs = this.jobs.map(j => jobId === j.id ? { ...j, status: JobStatus.IN_PROGRESS, endDate: null } : j)
    }

    disableJob(job) {
        this.getInstance().disableJob(job);
    }

    disableJobSuccess(response) {
        const jobId = response.jobId;
        this.jobs = this.jobs.map(j => jobId === j.id ? { ...j, enabled: false } : j)
    }

    enableJob(job) {
        this.getInstance().enableJob(job);
    }

    enableJobSuccess(response) {
        const jobId = response.jobId;
        this.jobs = this.jobs.map(j => jobId === j.id ? { ...j, enabled: true } : j);
    }

    setJobFilter(repos) {
        this.filter = repos;
    }
}

export default alt.createStore(JobStore, 'JobStore');
