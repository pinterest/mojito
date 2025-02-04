import React from "react";
import JobActions from "../../actions/jobs/JobActions";

class JobButton extends React.Component {

    handleClick = (job, type) => {
        switch(type) {
            case JobButton.TYPES.RUN:
                JobActions.triggerJob(job);
                break;
            case JobButton.TYPES.DISABLE:
                break;
            case JobButton.TYPES.ENABLE:
                break;
        }
    }

    /**
     * @return {XML}
     */
    render() {

        const type = this.props.type;
        const disabled = this.props.disabled;
        const job = this.props.job;

        const disabledStyle = {
            "backgroundColor": "#c2c2c257",
            "color" : "#a9a9a945",
            "border" : "1px solid #c2c2c242"
        }

        return (
            <button className="job-button" disabled={disabled}
                    style={disabled ? disabledStyle : {}} onClick={() => this.handleClick(job, type)} >
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
