import React from "react";
import PropTypes from "prop-types";
import { DropdownButton, MenuItem } from "react-bootstrap";
import cronstrue from "cronstrue";

const CRON_EXPRESSION_OPTIONS = [
    { display: "Every 5 Minutes", cron: "0 0/5 * * * ?" },
    { display: "Every Hour", cron: "0 0 * * * ?" },
    { display: "Every Day at Midnight", cron: "0 0 0 * * ?" },
    { display: "Custom Cron Expression", cron: "__custom__" },
];

class CronExpressionInput extends React.Component {
    static propTypes = {
        cron: PropTypes.string,
        onChange: PropTypes.func.isRequired,
    };

    constructor(props) {
        super(props);
        const matchedOption = CRON_EXPRESSION_OPTIONS.find(option => option.cron === props.cron);
        this.state = {
            selectedOption: props.cron
                ? (matchedOption ? matchedOption.cron : "__custom__")
                : null,
            customCron: matchedOption || !props.cron ? "" : (props.cron || "")
        };
    }

    componentDidUpdate(prevProps) {
        if (prevProps.cron !== this.props.cron) {
            const matchedOption = CRON_EXPRESSION_OPTIONS.find(option => option.cron === this.props.cron);
            if (this.state.selectedOption !== "__custom__" || (this.props.cron && matchedOption)) {
                this.setState({
                    selectedOption: this.props.cron
                        ? (matchedOption ? matchedOption.cron : "__custom__")
                        : null,
                    customCron: matchedOption || !this.props.cron ? "" : (this.props.cron || "")
                });
            }
        }
    }

    handleDropdownSelect = (eventKey) => {
        if (eventKey === "__custom__") {
            this.setState({ selectedOption: "__custom__", customCron: "" });
        } else {
            this.setState({ selectedOption: eventKey, customCron: "" });
            this.props.onChange({ target: { name: "cron", value: eventKey } });
        }
    };

    handleCustomCronChange = (e) => {
        const value = e.target.value;
        this.setState({ customCron: value });
        this.props.onChange({ target: { name: "cron", value } });
    };

    validateCronExpression = (cron) => {
        if (!cron) return false;
        const fields = cron.trim().split(/\s+/);
        if (fields.length !== 6) return false;
        const fieldDefs = [
            { name: 'Seconds',   re: /^[0-5]?[0-9]([,\-*/]?[0-5]?[0-9])*$/ },
            { name: 'Minutes',   re: /^[0-5]?[0-9]([,\-*/]?[0-5]?[0-9])*$/ },
            { name: 'Hours',     re: /^([01]?\d|2[0-3])([,\-*/]?([01]?\d|2[0-3]))*$/ },
            { name: 'DayOfMonth',re: /^([1-9]|[12]\d|3[01]|\?|L|W)([,\-*/]?([1-9]|[12]\d|3[01]|\?|L|W))*$/ },
            { name: 'Month',     re: /^(0?[1-9]|1[0-1]|JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)([,\-*/]?(0?[1-9]|1[0-1]|JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC))*$/i },
            { name: 'DayOfWeek', re: /^([1-7]|SUN|MON|TUE|WED|THU|FRI|SAT|\?|L|#)([,\-*/]?([1-7]|SUN|MON|TUE|WED|THU|FRI|SAT|\?|L|#))*$/i }
        ];
        for (let i = 0; i < 6; i++) {
            if (!fieldDefs[i].re.test(fields[i]) && fields[i] !== '*') {
                return false;
            }
        }
        return true;
    };

    getCustomCronDescription = (cron) => {
        if (!cron) { return }
        if (!this.validateCronExpression(cron)) {
            return <span style={{ color: 'red' }}>Invalid cron expression</span>;
        }
        try {
            return cronstrue.toString(cron);
        } catch (e) {
            return <span style={{ color: 'red' }}>Invalid cron expression</span>;
        }
    };

    render() {
        const { selectedOption, customCron } = this.state;
        let selectedDisplay = "Choose a Cron Expression";
        if (selectedOption === "__custom__") {
            selectedDisplay = "Custom Cron Expression";
        } else if (selectedOption) {
            selectedDisplay = (CRON_EXPRESSION_OPTIONS.find(option => option.cron === selectedOption) || {}).display;
        }
        return (
            <div>
                <DropdownButton
                    id="cron-expression-dropdown"
                    title={selectedDisplay}
                    onSelect={this.handleDropdownSelect}
                >
                    {CRON_EXPRESSION_OPTIONS.map(option => (
                        <MenuItem
                            key={option.cron}
                            eventKey={option.cron}
                            active={selectedOption === option.cron}
                        >
                            {option.display}
                        </MenuItem>
                    ))}
                </DropdownButton>
                {selectedOption === "__custom__" && (
                    <div>
                        <input
                            type="text"
                            className="form-control mts mbs"
                            placeholder="Enter Custom Cron Expression (e.g. 0 0/5 * * * ?)"
                            value={customCron}
                            onChange={this.handleCustomCronChange}
                            name="cron"
                        />
                        {this.getCustomCronDescription(customCron)}
                    </div>
                )}
            </div>
        );
    }
}

export default CronExpressionInput;