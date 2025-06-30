import React from "react";
import createReactClass from "create-react-class";
import PropTypes from "prop-types";
import { Button, Modal, Form } from "react-bootstrap";
import { JobType } from "../../utils/JobType";
import JobGeneralInput from "./JobGeneralInput";
import JobThirdPartyInput from "./JobThirdPartyInput";
import JobAdvancedInput from "./JobAdvancedInput";
import { validateCronExpression } from "../../utils/CronExpressionHelper";

const JobInputModal  = createReactClass({
    displayName: "JobInputModal",
    propTypes: {
        title: PropTypes.string.isRequired,
        show: PropTypes.bool.isRequired,
        onClose: PropTypes.func.isRequired,
        onSubmit: PropTypes.func.isRequired,
        job: PropTypes.object,
        errorMessage: PropTypes.string,
        isSubmitting: PropTypes.bool
    },

    getInitialState() {
        return {
            id: null,
            selectedRepository: null,
            jobType: JobType.THIRD_PARTY_SYNC,
            cron: "",
            thirdPartyProjectId: "",
            selectedActions: ["PUSH", "PULL", "MAP_TEXTUNIT", "PUSH_SCREENSHOT"],
            localeMapping: [],
            skipTextUnitsWithPattern: "",
            pluralSeparator: "",
            skipAssetsWithPathPattern: "",
            includeTextUnitsWithPattern: "",
            options: [],
            currentStep: 0
        };
    },

    componentDidUpdate(prevProps) {
        if (
            this.props.errorMessage &&
            this.props.errorMessage !== prevProps.errorMessage &&
            this.state.currentStep !== 0
        ) {
            this.setState({ currentStep: 0 });
        }
        if (prevProps.job !== this.props.job) {
            if (this.props.job) {
                this.setState({
                    id: this.props.job.id,
                    selectedRepository: this.props.job.repository || null,
                    jobType: this.props.job.type || JobType.THIRD_PARTY_SYNC,
                    cron: this.props.job.cron || "",
                    thirdPartyProjectId: this.props.job.properties.thirdPartyProjectId || "",
                    selectedActions: this.props.job.properties.actions || [],
                    localeMapping: this.parseLocaleMappingString(this.props.job.properties.localeMapping),
                    skipTextUnitsWithPattern: this.props.job.properties.skipTextUnitsWithPattern || "",
                    pluralSeparator: this.props.job.properties.pluralSeparator || "",
                    skipAssetsWithPathPattern: this.props.job.properties.skipAssetsWithPathPattern || "",
                    includeTextUnitsWithPattern: this.props.job.properties.includeTextUnitsWithPattern || "",
                    options: this.parseOptionsArray(this.props.job.properties.options || [])
                });
            } else {
                this.clearModal();
            }
        }
    },

    // Converts "en:en-US, fr: fr-FR" to [{key: 'en', value: 'en-US'}, {key: 'fr', value: 'fr-FR'}]
    parseLocaleMappingString(localeMappingString) {
        if (!localeMappingString || typeof localeMappingString !== 'string') return [];
        return localeMappingString.split(',').map(pair => {
            const [key, value] = pair.split(':');
            return {
                key: key ? key.trim() : '',
                value: value ? value.trim() : ''
            };
        }).filter(pair => pair.key || pair.value);
    },

    // Converts array of {key, value} to "en:en-US, fr:fr-FR"
    serializeLocaleMappingArray(localeMappingArray) {
        if (!Array.isArray(localeMappingArray)) return '';
        return localeMappingArray
            .filter(pair => pair.key && pair.value)
            .map(pair => `${pair.key}:${pair.value}`)
            .join(', ');
    },

    // Converts array of {key, value} to ["key=value", ...]
    serializeOptionsArray(optionsArray) {
        if (!Array.isArray(optionsArray)) return [];
        return optionsArray
            .filter(pair => pair.key && pair.value)
            .map(pair => `${pair.key}=${pair.value}`);
    },

    // Converts ["key=value", ...] to array of {key, value}
    parseOptionsArray(optionsList) {
        if (!Array.isArray(optionsList)) return [];
        return optionsList.map(option => {
            const [key, value] = option.split('=');
            return {
                key: key ? key.trim() : '',
                value: value ? value.trim() : ''
            };
        }).filter(pair => pair.key || pair.value);
    },

    getScheduledJobInput() {
        return {
            id: this.state.id,
            repository: this.state.selectedRepository,
            propertiesString: JSON.stringify({
                thirdPartyProjectId: this.state.thirdPartyProjectId,
                actions: this.state.selectedActions,
                pluralSeparator: this.state.pluralSeparator,
                localeMapping: this.serializeLocaleMappingArray(this.state.localeMapping),
                skipTextUnitsWithPattern: this.state.skipTextUnitsWithPattern,
                skipAssetsWithPathPattern: this.state.skipAssetsWithPathPattern,
                includeTextUnitsWithPattern: this.state.includeTextUnitsWithPattern,
                options: this.serializeOptionsArray(this.state.options)
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
            selectedActions: ["PUSH", "PULL", "MAP_TEXTUNIT", "PUSH_SCREENSHOT"],
            localeMapping: [],
            skipTextUnitsWithPattern: "",
            pluralSeparator: "",
            skipAssetsWithPathPattern: "",
            includeTextUnitsWithPattern: "",
            options: [],
            currentStep: 0
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

    handleLocaleMappingChange(newMapping) {
        this.setState({ localeMapping: newMapping });
    },

    handleOptionsMappingChange(newOptions) {
        this.setState({ options: newOptions });
    },

    handleSubmit(e) {
        e.preventDefault();
        const scheduledJobInput = this.getScheduledJobInput();
        this.props.onSubmit(scheduledJobInput);
        this.setState({ currentStep: 0 });
    },

    isStepValid(step) {
        if (step === 0) {
            return this.state.selectedRepository &&
                this.state.jobType &&
                this.state.cron
                validateCronExpression(this.state.cron);
        }
        if (step === 1) {
            return this.state.thirdPartyProjectId &&
                Array.isArray(this.state.selectedActions) &&
                this.state.selectedActions.length > 0;
        }
        return true;
    },

    renderStepContent() {
        const { currentStep } = this.state;
        switch (currentStep) {
            case 0:
                return (
                    <JobGeneralInput
                        selectedRepository={this.state.selectedRepository}
                        onRepositorySelect={this.handleRepositorySelect}
                        jobType={this.state.jobType}
                        onJobTypeChange={this.handleJobTypeChange}
                        cron={this.state.cron}
                        onInputChange={this.onHandleInputChange}
                        getLabelInputTextBox={this.getLabelInputTextBox}
                    />
                );
            case 1:
                return (
                    <JobThirdPartyInput
                        thirdPartyProjectId={this.state.thirdPartyProjectId}
                        onInputChange={this.onHandleInputChange}
                        selectedActions={this.state.selectedActions}
                        onActionsChange={this.handleActionsChange}
                        localeMapping={this.state.localeMapping}
                        onLocaleMappingChange={this.handleLocaleMappingChange}
                        getLabelInputTextBox={this.getLabelInputTextBox}
                    />
                );
            case 2:
                return (
                    <JobAdvancedInput
                        pluralSeparator={this.state.pluralSeparator}
                        skipTextUnitsWithPattern={this.state.skipTextUnitsWithPattern}
                        skipAssetsWithPathPattern={this.state.skipAssetsWithPathPattern}
                        includeTextUnitsWithPattern={this.state.includeTextUnitsWithPattern}
                        options={this.state.options}
                        onInputChange={this.onHandleInputChange}
                        onOptionsMappingChange={this.handleOptionsMappingChange}
                        getLabelInputTextBox={this.getLabelInputTextBox}
                    />
                );
            default:
                return null;
        }
    },

    handleNextStep() {
        this.setState((prevState) => ({ currentStep: prevState.currentStep + 1 }));
    },

    handlePrevStep() {
        this.setState((prevState) => ({ currentStep: prevState.currentStep - 1 }));
    },

    render() {
        const { currentStep } = this.state;
        const isLastStep = currentStep === 2;
        const isFirstStep = currentStep === 0;
        return (
            <Modal show={this.props.show} onHide={this.props.onClose}>
                <Form
                    onSubmit={this.handleSubmit}
                >
                    <Modal.Header closeButton>
                        <Modal.Title>{this.props.title}</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        {this.props.errorMessage && (
                            <div className="alert alert-danger">
                                {this.props.errorMessage}
                            </div>
                        )}
                        {this.renderStepContent()}
                    </Modal.Body>
                    <Modal.Footer>
                        <Button variant="secondary" onClick={this.props.onClose}>
                            Close
                        </Button>
                        {!isFirstStep && (
                            <Button variant="secondary" onClick={this.handlePrevStep}>
                                Back
                            </Button>
                        )}
                        {!isLastStep && (
                            <Button
                                variant="primary"
                                onClick={this.handleNextStep}
                                disabled={!this.isStepValid(currentStep) || this.props.isSubmitting}
                            >
                                Next
                            </Button>
                        )}
                        {isLastStep && (
                            <Button
                                variant="primary"
                                type="submit"
                                disabled={!this.isStepValid(currentStep) || this.props.isSubmitting}
                            >
                                {this.props.isSubmitting ? "Submitting..." : "Submit"}
                            </Button>
                        )}
                    </Modal.Footer>
                </Form>
            </Modal>
        );
    }
});

export default JobInputModal;