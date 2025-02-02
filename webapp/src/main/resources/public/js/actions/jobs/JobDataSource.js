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
        success: JobActions.triggerJobSuccess,
        error: JobActions.triggerJobError
    }
};

export default JobDataSource;
