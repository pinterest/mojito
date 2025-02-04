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
        );
    }
}

export default alt.createActions(JobActions);
