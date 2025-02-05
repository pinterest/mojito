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
            "disableJob",
            "enableJob",
            "setJobStatus",
            "setJobFilter"
        );
    }
}

export default alt.createActions(JobActions);
