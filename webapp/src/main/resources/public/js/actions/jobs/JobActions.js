import alt from "../../alt";

class JobActions {
    constructor() {
        this.generateActions(
            "getAllJobs",
            "getAllJobsSuccess",
            "getAllJobsError",
            "triggerJob",
            "triggerJobSuccess",
            "triggerJobError",
            "setJobStatus",
        );
    }
}

export default alt.createActions(JobActions);
