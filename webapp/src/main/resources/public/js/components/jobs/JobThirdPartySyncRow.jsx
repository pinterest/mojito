import React from "react";
import {withAppConfig} from "../../utils/AppConfig";
import {MdSyncAlt} from "react-icons/md";
import JobStatusLabel from "./JobStatusLabel";
import JobButton from "./JobButton";
import {ImSpinner2} from "react-icons/im";
import {JobStatus} from "../../utils/JobStatus";
import PropTypes from "prop-types";
import AuthorityService from "../../utils/AuthorityService";

class JobThirdPartySyncRow extends React.Component {

    static propTypes = {
        "job": PropTypes.object.isRequired,
        "openEditJobModal": PropTypes.func.isRequired,
        "hideThirdPartyLink": PropTypes.bool,
    }

    constructor(props) {
        super(props);
        this.state = {
            nextStartMessage: '',
        };
    }

    componentDidMount() {
        this.updateNextStartMessage();
        this.intervalId = setInterval(this.updateNextStartMessage, 1000);
    }

    componentWillUnmount() {
        clearInterval(this.intervalId);
    }

    componentDidUpdate(prevProps, prevState) {
        if (prevProps.job.nextStartDate !== this.props.job.nextStartDate) {
            this.updateNextStartMessage();
        }
    }

    updateNextStartMessage = () => {
        const difference = (this.props.job.nextStartDate - Date.now()) / 1000;

        if (difference <= 0) {
            this.setState({ nextStartMessage: 'Starting...' });
            clearInterval(this.intervalId);
        } else {
            const minutesLeft = Math.ceil(difference / 60);
            if(minutesLeft > 1) {
                this.setState({ nextStartMessage: `Scheduled to run in ${minutesLeft} minutes` });
            } else {
                this.setState({ nextStartMessage: `Scheduled to run in ${minutesLeft} minute` });
            }


            if (this.intervalId === null) {
                this.intervalId = setInterval(this.updateNextStartMessage, 1000);
            }
        }
    };

    convertUnixToDate(unixTime) {
        return new Intl.DateTimeFormat('en-US', {
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit',
            hour12: false
        }).format(new Date(unixTime));
    }

    getThirdPartyLink(job) {
        if (this.props.hideThirdPartyLink) {
            return;
        }
        if(job.repository && job.repository in APP_CONFIG.link) {
            const url = new URL(APP_CONFIG.link[job.repository].thirdParty.url);
            url.search = '';
            return <span> - <a href={url}>{(job.properties && job.properties.thirdPartyProjectId) || ""}</a></span>;
        }
        return <span> - {(job.properties && job.properties.thirdPartyProjectId) || ""}</span>;
    }

    getJobStatusLabelStatus(job) {
        if (job.deleted) return "DELETED";
        if (!job.enabled) return "DISABLED";
        return job.status;
    }

    /**
     * @return {XML}
     */
    render() {

        const { job } = this.props;
        const inProgress = job.status === JobStatus.IN_PROGRESS;
        const jobTypeFormatted = job.type.replaceAll('_', ' ').toLowerCase()
            .split(' ')
            .map(word => word.charAt(0).toUpperCase() + word.slice(1))
            .join(' ')

        return (
            <div className="job-row">
                <div className="job-inner-row">
                    <MdSyncAlt />
                    <div className="job-details">
                        <div className="job-details-title">
                            <h1 className={inProgress ? "job-details-title-loading" : ""}>
                            { job.type && jobTypeFormatted }
                            </h1>
                            <JobStatusLabel status={this.getJobStatusLabelStatus(job)} />
                        </div>
                        <div>{job.repository}{this.getThirdPartyLink(job)}</div>
                    </div>
                    <div className="job-timings">
                        <div>Started @ {job.startDate && this.convertUnixToDate(job.startDate)}</div>
                        <div>Ended @ {job.endDate ? this.convertUnixToDate(job.endDate) : "..."}</div>
                    </div>
                </div>
                <div className="job-bottom-row">
                    {inProgress ?
                        <div className="job-next-run-info">
                            <ImSpinner2/>
                            <div>Running ...</div>
                            {!job.enabled && <div>(Job will not run again)</div>}
                        </div>
                        :
                        job.deleted ?
                            <div>
                                <div>Deleted</div>
                            </div>
                        :
                        job.enabled ?
                            <div>
                                <div>{this.state.nextStartMessage}</div>
                            </div>
                        :
                            <div>
                                <div>Disabled</div>
                            </div>
                    }

                    <div className="job-controls">
                        {AuthorityService.canTriggerEnableDisableJobs() &&
                            <JobButton job={job}
                                type={JobButton.TYPES.RUN} disabled={inProgress || !job.enabled || job.deleted}
                            />
                        }
                        {AuthorityService.canTriggerEnableDisableJobs() &&
                            <JobButton job={job}
                                type={ job.enabled ? JobButton.TYPES.DISABLE : JobButton.TYPES.ENABLE}
                                disabled={job.deleted}
                            />
                        }
                        <JobButton job={job}
                                   type={JobButton.TYPES.EDIT}
                                   openEditJobModal={this.props.openEditJobModal}
                                   disabled={job.deleted}
                        />
                        {AuthorityService.canDeleteRestoreJobs() &&
                            <JobButton job={job}
                                type={ job.deleted ? JobButton.TYPES.RESTORE : JobButton.TYPES.DELETE}
                            />
                        }
                    </div>
                </div>
            </div>
        );
    }
}

export default withAppConfig(JobThirdPartySyncRow);
