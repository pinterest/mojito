import React from "react";
import JobActions from "../../actions/jobs/JobActions";
import PropTypes from "prop-types";


class JobButton extends React.Component {

    static propTypes = {
        "job": PropTypes.object.isRequired,
        "type": PropTypes.string.isRequired,
    }

    handleClick = (job, type, button) => {
        // Disable the button to avoid register spam clicks
        button.disabled = true;

        switch(type) {
            case JobButton.TYPES.RUN:
                JobActions.triggerJob(job);
                break;
            case JobButton.TYPES.DISABLE:
                JobActions.disableJob(job);
                break;
            case JobButton.TYPES.ENABLE:
                JobActions.enableJob(job);
                break;
        }

        setTimeout(() => {
            // Wait before switching on the button to avoid double clicks
            button.disabled = false;
        }, 500)

    }

    /**
     * @return {XML}
     */
    render() {

        const {type, disabled, job} = this.props;

        return (
            <button className={`job-button ${disabled ? 'disabled' : ''}`} disabled={disabled}
                    onClick={(e) => this.handleClick(job, type, e.currentTarget)} >
                {type}
            </button>
        );
    }
}

JobButton.TYPES = {
    "RUN": "RUN",
    "DISABLE": "DISABLE",
    "ENABLE": "ENABLE"
}

export default JobButton;
