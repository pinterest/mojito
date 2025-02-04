import React from "react";
import {withAppConfig} from "../../utils/AppConfig";
import JobRow from "./JobRow";
import JobStore from "../../stores/jobs/JobStore";
import JobActions from "../../actions/jobs/JobActions";

class JobsView extends React.Component {

    constructor(props) {
        super(props);

        this.state = JobStore.getState();

        // Bind this to the function call to ensure this references this instance.
        this.jobStoreChange = this.jobStoreChange.bind(this);
        JobActions.getAllJobs();

        this.interval = setInterval(() => {
            JobActions.getAllJobs();
        }, 5000);
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
