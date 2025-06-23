import React from "react";
import createReactClass from "create-react-class";
import { Button } from "react-bootstrap";
import ScheduledJobInputModal from "./ScheduledJobInputModal";
import JobActions from "../../actions/jobs/JobActions";

const CreateJobButton = createReactClass({
    displayName: "CreateJobButton",
    getInitialState() {
        return {
            modalOpen: false
        }
    },
    openModal() {
        this.setState({ modalOpen: true });
    },
    closeModal() {
        this.setState({ modalOpen: false });
    },
    render() {
        return (
            <div>
                <Button bsStyle="primary" onClick={this.openModal}>
                    Create Job
                </Button>
                <ScheduledJobInputModal show={this.state.modalOpen} onClose={this.closeModal} onSubmit={JobActions.createJob} />
            </div>
        );
    }
});

export default CreateJobButton;