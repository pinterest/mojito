import BaseClient from "./BaseClient";
import JobActions from "../actions/jobs/JobActions";
class JobClient extends BaseClient {

    getJobs() {
        return this.get(this.getUrl(), {});
    }

    triggerJob(job) {
        const jobTriggerUrl = `/${job.id}/trigger`
        return this.post(this.getUrl() + jobTriggerUrl, null)
            .then(resp => {
                setTimeout(() => {
                    JobActions.getAllJobs();
                }, 500);
            });
    }

    getEntityName() {
        return 'jobs';
    }
}

export default new JobClient();