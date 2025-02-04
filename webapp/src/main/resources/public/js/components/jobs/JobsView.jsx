import React from "react";
import {withAppConfig} from "../../utils/AppConfig";
import JobRow from "./JobRow";
import JobStore from "../../stores/jobs/JobStore";
import JobActions from "../../actions/jobs/JobActions";
import AuthorityService from "../../utils/AuthorityService";
import {FormattedMessage} from "react-intl";

class JobsView extends React.Component {

    constructor(props) {
        super(props);

        this.state = JobStore.getState();

        // Bind this to the function call to ensure 'this' references the current instance.
        this.jobStoreChange = this.jobStoreChange.bind(this);

        if(AuthorityService.canViewJobs()) {
            // Only poll if we have access, stops polling for 403 responses
            JobActions.getAllJobs();
            this.interval = setInterval(() => {
                JobActions.getAllJobs();
            }, 5000);
        }
    }

    componentDidMount() {
        // Start listening the JobStore updates
        JobStore.listen(this.jobStoreChange);
    }

    componentWillUnmount() {
        // Stop listening to JobStore and clear the polling loop
        JobStore.unlisten(this.jobStoreChange);
        clearInterval(this.interval);
    }

    jobStoreChange(state) {
        // Any change to the JobStore will come here
        this.setState({jobs: state.jobs})
    }

    /**
     * @return {XML}
     */
    render() {
        if (!AuthorityService.canViewJobs()) {
            return (
                <div className="ptl">
                    <h3 className="text-center mtl">You do not have permissions to view Jobs.</h3>
                </div>
            )
        }

        return (
            <div>
                <div className="jobs-container">
                    {
                        this.state.jobs.map(job =>
                            <JobRow job={job} />
                        )
                    }
                </div>
            </div>
        );
    }
}

export default withAppConfig(JobsView);
