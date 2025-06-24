import React from "react";
import createReactClass from "create-react-class";
import PropTypes from "prop-types";
import { Button, Modal, Form, Tabs, Tab } from "react-bootstrap";
import CreateJobRepositoryDropDown from "./CreateJobRepositoryDropDown";
import { JobType } from "../../utils/JobType";
import JobTypeDropdown from "./JobTypeDropdown";
import ThirdPartySyncActionsInput from "./ThirdPartySyncActionsInput";

const ScheduledJobInputModal  = createReactClass({
    displayName: "ScheduledJobInputModal",
    propTypes: {
        show: PropTypes.bool.isRequired,
        onClose: PropTypes.func.isRequired,
        onSubmit: PropTypes.func.isRequired,
        job: PropTypes.object
    },


    getInitialState() { 
        return {
            id: null,
            selectedRepository: null,
            jobType: JobType.THIRD_PARTY_SYNC,
            cron: "",
            thirdPartyProjectId: "",
            selectedActions: [],
            localeMapping: "",
            skipTextUnitsWithPattern: "",
            pluralSeparator: "",
            skipAssetsWithPathPattern: "",
            includeTextUnitsWithPattern: ""
        };
    },

    componentDidUpdate(prevProps) {
        if (prevProps.job !== this.props.job) {
            if (this.props.job) {
                this.setState({
                    id: this.props.job.id,
                    selectedRepository: this.props.job.repository,
                    jobType: this.props.job.type,
                    cron: this.props.job.cron,
                    thirdPartyProjectId: this.props.job.properties.thirdPartyProjectId || "",
                    selectedActions: this.props.job.properties.actions || [],
                    localeMapping: this.props.job.properties.localeMapping || "",
                    skipTextUnitsWithPattern: this.props.job.properties.skipTextUnitsWithPattern || "",
                    pluralSeparator: this.props.job.properties.pluralSeparator || "",
                    skipAssetsWithPathPattern: this.props.job.properties.skipAssetsWithPathPattern || "",
                    includeTextUnitsWithPattern: this.props.job.properties.includeTextUnitsWithPattern || ""
                });
            } else {
                this.clearModal();
            }
        }
    },

    getScheduledJobInput() {
        return {
            id: this.state.id,
            repository: this.state.selectedRepository,
            propertiesString: JSON.stringify({
                thirdPartyProjectId: this.state.thirdPartyProjectId,
                actions: this.state.selectedActions,
                pluralSeparator: this.state.pluralSeparator,
                localeMapping: this.state.localeMapping,
                skipTextUnitsWithPattern: this.state.skipTextUnitsWithPattern,
                skipAssetsWithPathPattern: this.state.skipAssetsWithPathPattern,
                includeTextUnitsWithPattern: this.state.includeTextUnitsWithPattern,
                options: ["smartling-placeholder-format=NONE"]
            }),
            cron: this.state.cron,
            type: this.state.jobType,
        };
    },

    clearModal() {
        this.setState({
            id: null,
            selectedRepository: null,
            jobType: JobType.THIRD_PARTY_SYNC,
            cron: "",
            thirdPartyProjectId: "",
            selectedActions: [],
            localeMapping: "",
            skipTextUnitsWithPattern: "",
            pluralSeparator: "",
            skipAssetsWithPathPattern: "",
            includeTextUnitsWithPattern: ""
        });
    },


    onHandleInputChange(e) {
        const { name, value } = e.target;
        this.setState({ [name]: value });
    },

    handleRepositorySelect(selectedRepository) {
        this.setState({ selectedRepository });
    },

    handleActionsChange(actions) {
        this.setState({ selectedActions: actions });
    },

    handleJobTypeChange(jobType) {
        this.setState({jobType: jobType})
    },

    getLabelInputTextBox(label, placeholder, inputName) {
        return (
            <div className="form-group mbm">
                <label>{label}</label>
                <input
                    className="form-control"
                    type="text"
                    name={inputName}
                    placeholder={placeholder}
                    value={this.state[inputName]}
                    onChange={this.onHandleInputChange}
                />
            </div>
        );
    },

    handleSubmit(e) {
        e.preventDefault();
        const scheduledJobInput = this.getScheduledJobInput();
        this.props.onSubmit(scheduledJobInput);
        this.props.onClose();
        this.clearModal();
    },

    render() {
        return (
            <Modal show={this.props.show} onHide={this.props.onClose}>
                <Form
                    onSubmit={this.handleSubmit}
                >
                    <Modal.Header closeButton>
                        <Modal.Title>Create a Scheduled Job</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        <Tabs defaultActiveKey="general" id="scheduled-job-tabs">
                            <Tab eventKey="general" title="General">
                                <div className="form-group mtm mbm">
                                    <label>Repository</label>
                                    <CreateJobRepositoryDropDown
                                        selected={this.state.selectedRepository}
                                        onSelect={this.handleRepositorySelect}
                                    />
                                </div>
                                <div className="form-group mtm mbm">
                                    <label>Job Type</label>
                                    <JobTypeDropdown onJobTypeChange={this.handleJobTypeChange} />
                                </div>
                                {this.getLabelInputTextBox("Sync Frequency (Cron)", "Enter cron expression", "cron")}
                            </Tab>
                            <Tab eventKey="advanced" title="Smartling">
                                <div className="form-group mtm">
                                    {this.getLabelInputTextBox("Third Party Project ID", "Enter Smartling Project Id", "thirdPartyProjectId")}
                                    <ThirdPartySyncActionsInput
                                        selectedActions={this.state.selectedActions}
                                        onChange={this.handleActionsChange}
                                    />
                                    {this.getLabelInputTextBox("Locale Mapping", "Enter locale mapping", "localeMapping")}
                                </div>
                            </Tab>
                            <Tab eventKey="options" title="Advanced">
                                <div className="form-group mtm">
                                    {this.getLabelInputTextBox("Plural Separator", "Enter plural separator", "pluralSeparator")}
                                    {this.getLabelInputTextBox("Skip Text Units With Pattern", "Enter skip text units pattern", "skipTextUnitsWithPattern")}
                                    {this.getLabelInputTextBox("Skip Assets With Path Pattern", "Enter skip assets with path pattern", "skipAssetsWithPathPattern")}
                                    {this.getLabelInputTextBox("Include Text Units With Pattern", "Enter include text units pattern", "includeTextUnitsWithPattern")}
                                </div>
                            </Tab>
                        </Tabs>
                    </Modal.Body>
                    <Modal.Footer>
                        <Button variant="secondary" onClick={this.props.onClose}>
                            Close
                        </Button>
                        <Button
                            variant="primary"
                            type="submit"
                            disabled={!this.state.selectedRepository}
                        >
                            Submit
                        </Button>
                    </Modal.Footer>
                </Form>
            </Modal>
        );
    }
});

export default ScheduledJobInputModal;