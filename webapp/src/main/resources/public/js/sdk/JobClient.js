import BaseClient from "./BaseClient";
import JobActions from "../actions/jobs/JobActions";
import JobStore from "../stores/jobs/JobStore";
import {JobStatus} from "../utils/JobStatus";

class JobClient extends BaseClient {

    getJobs() {
        return this.get(this.getUrl(), {});
    }

    triggerJob(job) {
        const jobTriggerUrl = `/${job.id}/trigger`
        return this.post(this.getUrl() + jobTriggerUrl, null)
            .then(resp => {
                JobActions.setJobStatus(job, JobStatus.IN_PROGRESS);
            });
    }

    disableJob(job) {
        const jobDisableUrl = `/${job.id}/disable`
        return this.post(this.getUrl() + jobDisableUrl, null)
            .then(resp => {
                JobActions.setJobStatus(job, JobStatus.DISABLED);
            });
    }

    enableJob(job) {
        const jobEnableUrl = `/${job.id}/enable`
        return this.post(this.getUrl() + jobEnableUrl, null)
            .then(resp => {
                JobActions.setJobStatus(job, JobStatus.ENABLED);
            });
    }

    getEntityName() {
        return 'jobs';
    }
}

export default new JobClient();