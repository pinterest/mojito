import React from "react";
import JobActions from "../../actions/jobs/JobActions";

class JobButton extends React.Component {

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
    }

    /**
     * @return {XML}
     */
    render() {

        const {type, disabled, job} = this.props;

        const disabledStyle = {
            "backgroundColor": "#c2c2c257",
            "color" : "#a9a9a945",
            "border" : "1px solid #c2c2c242"
        }

        return (
            <button className="job-button" disabled={disabled}
                    style={disabled ? disabledStyle : {}} onClick={(e) => this.handleClick(job, type, e.currentTarget)} >
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
