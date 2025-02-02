import React from "react";

class JobButton extends React.Component {

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
                    style={disabled ? disabledStyle : {}} onClick={() => this.props.click(job)} >
                {type}
            </button>
        );
    }
}

export default JobButton;
