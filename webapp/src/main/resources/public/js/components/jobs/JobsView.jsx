import React from "react";
import {withAppConfig} from "../../utils/AppConfig";
import JobRow from "./JobRow";
import JobStore from "../../stores/jobs/JobStore";
import JobActions from "../../actions/jobs/JobActions";

class JobsView extends React.Component {

    constructor(props) {
        super(props);

        this.state = JobStore.getState();
        // Bind this to the function call to unsure this references this instance.
        this.jobStoreChange = this.jobStoreChange.bind(this);

        // this.state = {
        //     jobs: [
        //         {
        //             repository: "pinboard",
        //             jobType: "THIRD PARTY SYNC",
        //             jobStatus: "SUCCEEDED",
        //             startDate: 1738345560000,
        //             endDate: 1738345590000,
        //             nextStart: 1738359615,
        //             enabled: true
        //         },
        //         {
        //             repository: "webapp",
        //             jobType: "THIRD PARTY SYNC",
        //             jobStatus: "SUCCEEDED",
        //             startDate: 1738345560000,
        //             endDate: 1738345590000,
        //             nextStart: 1738359615,
        //             enabled: true
        //         },
        //         {
        //             repository: "android",
        //             jobType: "THIRD PARTY SYNC",
        //             jobStatus: "FAILED",
        //             startDate: 1738345560000,
        //             endDate: 1738345590000,
        //             nextStart: 1738359615,
        //             enabled: true
        //         },
        //         {
        //             repository: "ios",
        //             jobType: "THIRD PARTY SYNC",
        //             jobStatus: "SUCCEEDED",
        //             startDate: 1738345560000,
        //             endDate: 1738345590000,
        //             nextStart: 1738359615,
        //             enabled: false
        //         }
        //     ],
        // };

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

    runClick = (job) => {
        JobActions.triggerJob(job);
    }

    disableEnableClick = (job) => {
        this.setState(prevState => {
            const newJobs = prevState.jobs.map(j => {
                if(j === job) {
                    return {...j, enabled: !j.enabled}
                }
                return j;
            })
            return {jobs: newJobs}
        })
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
                            <JobRow job={job} runClick={this.runClick} disableEnableClick={this.disableEnableClick} />
                        )
                    }
                </div>
            </div>
        );
    }
}

export default withAppConfig(JobsView);
