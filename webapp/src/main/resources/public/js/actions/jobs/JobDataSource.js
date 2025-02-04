import JobClient from "../../sdk/JobClient";
import JobActions from "./JobActions";

const JobDataSource = {
    getAllJobs: {
        remote() {
            return JobClient.getJobs();
        },

        success: JobActions.getAllJobsSuccess,
        error: JobActions.getAllJobsError
    },
    triggerJob: {
        remote(state, job) {
            return JobClient.triggerJob(job);
        },
        success: null,
        error: JobActions.triggerJobError
    },
    disableJob: {
        remote(state, job) {
            return JobClient.disableJob(job);
        },
        success: null,
        error: JobActions.disableJobError
    },
    enableJob: {
        remote(state, job) {
            return JobClient.enableJob(job);
        },
        success: null,
        error: JobActions.enableJobError
    }
};

export default JobDataSource;
