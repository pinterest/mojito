import React from "react";
import createReactClass from "create-react-class";
import PropTypes from "prop-types";
import { Button, Modal, Form } from "react-bootstrap";
import CreateJobRepositoryDropDown from "./CreateJobRepositoryDropDown";
import JobActions from "../../actions/jobs/JobActions";
import { JobType } from "../../utils/JobType";
import JobTypeDropdown from "./JobTypeDropdown";
import ThirdPartySyncActionsInput from "./ThirdPartySyncActionsInput";

const CreateJobModal = createReactClass({
    displayName: "CreateJobModal",
    propTypes: {
        show: PropTypes.bool.isRequired,
        onClose: PropTypes.func.isRequired,
        job: PropTypes.object
    },


    getInitialState() {
        return {
            selectedRepository: null,
            jobType: JobType.THIRD_PARTY_SYNC,
            cron: "0 0 0 * * ?",
            thirdPartyProjectId: "",
            skipTextUnitsWithPattern: "",
            pluralSeparator: "",
            skipAssetsWithPathPattern: "",
            includeTextUnitsWithPattern: "",
            localeMapping: "",
            selectedActions: []
        };
    },

    onHandleInputChange(e) {
        const { name, value } = e.target;
        this.setState({ [name]: value });
    },

    onHandleInputChange(e) {
        const { name, value } = e.target;
        this.setState({ [name]: value });
    },

    handleRepositorySelect(selectedRepository) {
        this.setState({ selectedRepository });
    },

    getScheduledJobInput() {
        return {
            repository: "Demo",
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

    handleSubmit(e) {
        e.preventDefault();
        // console.log(job);
        const scheduledJobInput = this.getScheduledJobInput();
        JobActions.createJob(scheduledJobInput);
        this.setState({ selectedRepository: null });
        this.props.onClose();
    },

    handleActionsChange(actions) {
        this.setState({ selectedActions: actions });
    },

    onJobTypeChange(jobType) {
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
                        <div className="form-group mbm">
                            <label>Repository</label>
                            <CreateJobRepositoryDropDown
                                selected={this.state.selectedRepository}
                                onSelect={this.handleRepositorySelect}
                            />
                        </div>
                        <div className="form-group mbm">
                            <label>Job Type</label>
                            <div>
                                <JobTypeDropdown onJobTypeChange={this.onJobTypeChange} />
                            </div>
                        </div>

                        <ThirdPartySyncActionsInput
                            selectedActions={this.state.selectedActions}
                            onChange={this.handleActionsChange}
                        />
                        {this.getLabelInputTextBox("Sync Frequency (Cron)", "Enter cron expression", "cron")}
                        {this.getLabelInputTextBox("Third Party Project ID", "Enter Smartling Project Id", "thirdPartyProjectId")}
                        {this.getLabelInputTextBox("Locale Mapping", "Enter locale mapping", "localeMapping")}
                        {this.getLabelInputTextBox("Plural Separator", "Enter plural separator", "pluralSeparator")}
                        {this.getLabelInputTextBox("Skip Text Units With Pattern", "Enter skip text units pattern", "skipTextUnitsWithPattern")}
                        {this.getLabelInputTextBox("Skip Assets With Path Pattern", "Enter skip assets with path pattern", "skipAssetsWithPathPattern")}
                        {this.getLabelInputTextBox("Include Text Units With Pattern", "Enter include text units pattern", "includeTextUnitsWithPattern")}
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

export default CreateJobModal;