import alt from "../../alt";

class JobActions {
    constructor() {
        this.generateActions(
            "getAllJobs",
            "getAllJobsSuccess",
            "getAllJobsError",
            "triggerJob",
            "triggerJobSuccess",
            "triggerJobError"
        );
    }
}

export default alt.createActions(JobActions);
