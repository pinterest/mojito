import React from "react";
import {withAppConfig} from "../../utils/AppConfig";

class JobStatusLabel extends React.Component {

    /**
     * @return {XML}
     */
    render() {

        const status = this.props.status;

        const styleConfig = {
            "IN_PROGRESS" : {
                "backgroundColor": "rgba(147, 112, 219, 0.24)",
                "color": "mediumpurple"
            },
            "SUCCEEDED" : {
                "backgroundColor": "rgba(85, 151, 69, 0.25)",
                "color": "#559745"
            },
            "FAILED" : {
                "backgroundColor": "rgba(251, 52, 52, 0.25)",
                "color": "rgb(251, 52, 52)"
            },
            "DISABLED" : {
                "backgroundColor": "rgba(182, 182, 182, 0.25)",
                "color": "#656565"
            }
        }

        return (
            <div className={"job-status"} style={styleConfig[status]}>
                {status && status.replace('_', ' ')}
            </div>
        );
    }
}

export default withAppConfig(JobStatusLabel);
