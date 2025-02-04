import React from "react";
import {withAppConfig} from "../../utils/AppConfig";
import {MdSyncAlt} from "react-icons/md";
import JobStatusLabel from "./JobStatusLabel";
import JobButton from "./JobButton";
import {ImSpinner2} from "react-icons/im";

class JobRow extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            timeLeft: '',
        };
    }

    componentDidMount() {
        this.updateTimeLeft();
        this.intervalId = setInterval(this.updateTimeLeft, 1000);
    }

    componentWillUnmount() {
        clearInterval(this.intervalId);
    }

    componentDidUpdate(prevProps, prevState) {
        if (prevProps.job.nextStartDate !== this.props.job.nextStartDate) {
            this.updateTimeLeft();
        }
    }

    updateTimeLeft = () => {
        const difference = (this.props.job.nextStartDate - Date.now()) / 1000;

        if (difference <= 0) {
            this.setState({ timeLeft: 'Starting...' });
            clearInterval(this.intervalId);
        } else {
            const minutesLeft = Math.ceil(difference / 60);
            if(minutesLeft > 1) {
                this.setState({ timeLeft: `Running in ${minutesLeft} minutes` });
            } else {
                this.setState({ timeLeft: `Running in ${minutesLeft} minute` });
            }


            if (this.intervalId === null) {
                this.intervalId = setInterval(this.updateTimeLeft, 1000);
            }
        }
    };

    convertUnixToDate(unixTime) {
        const date = new Date(unixTime);

        const month = date.toLocaleString('default', { month: 'long' }).substring(0, 3);
        const day = date.getUTCDay();

        const hours = date.getUTCHours();
        const minutes = date.getUTCMinutes();
        const seconds = date.getUTCSeconds();

        // Pad with zeros if single digit
        const time = [
            hours.toString().padStart(2, '0'),
            minutes.toString().padStart(2, '0'),
            seconds.toString().padStart(2, '0')
        ].join(':');

        return `${month} ${day}, ${time}`;
    }


    /**
     * @return {XML}
     */
    render() {

        const job = this.props.job;
        const inProgress = job.status === "IN_PROGRESS";

        return (
            <div className="job-row">
                <div className="job-inner-row">
                    <MdSyncAlt />
                    <div className="job-details">
                        <div className="job-details-title">
                            <h1 className={inProgress ? "job-details-title-loading" : ""}>
                            {
                                job.type && job.type
                                    .replaceAll('_', ' ').toLowerCase()
                                    .split(' ')
                                    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
                                    .join(' ')
                            }
                            </h1>
                            <JobStatusLabel status={job.enabled ? job.status : "DISABLED"} />
                        </div>
                        <div>{job.repository}</div>
                    </div>
                    <div className="job-timings">
                        <div>Started @ {job.startDate && this.convertUnixToDate(job.startDate)}</div>
                        <div>Ended @ {job.endDate ? this.convertUnixToDate(job.endDate) : "..."}</div>
                    </div>
                </div>
                <div className="job-bottom-row">
                    {job.enabled && inProgress &&
                        <div className="job-next-run-info">
                            <ImSpinner2/>
                            <div>Running ...</div>
                        </div>
                    }

                    {job.enabled && !inProgress &&
                        <div>
                            <div>{this.state.timeLeft}</div>
                        </div>
                    }

                    {!job.enabled && <div>Disabled</div>}

                    <div className="job-controls">
                        <JobButton job={job} type={"RUN"} disabled={inProgress || !job.enabled} click={this.props.runClick} />
                        <JobButton job={job} type={ job.enabled ? "DISABLE" : "ENABLE"} click={this.props.disableEnableClick} />
                    </div>
                </div>
            </div>
        );
    }
}

export default withAppConfig(JobRow);
