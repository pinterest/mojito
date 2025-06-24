import React from "react";
import createReactClass from 'create-react-class';
import {withRouter} from "react-router";
import JobTypeDropdown from "./JobTypeDropdown";
import JobsView from "./JobsView";
import AltContainer from "alt-container";
import JobStore from "../../stores/jobs/JobStore";
import RepositoryDropDown from "./RepositoryDropDown";
import { Button } from "react-bootstrap";
import JobActions from "../../actions/jobs/JobActions";
import ScheduledJobInputModal from "./ScheduledJobInputModal";

let JobsPage = createReactClass({
    displayName: 'JobsPage',

    getInitialState() {
      return {
          jobType: null,
          showScheduledJobInputModal: false,
          editingJob: null
      };
    },

    openCreateJobModal() {
        this.setState({showScheduledJobInputModal: true});
    },

    closeCreateJobModal() {
        this.setState({showScheduledJobInputModal: false});
    },

    handleCreateJobSubmit(job) {
        JobActions.createJob(job);
        this.closeCreateJobModal();
    },

    openEditJobModal(job) {
        this.setState({
            showScheduledJobInputModal: true,
            editingJob: job
        });
    },

    closeEditJobModal() {
        this.setState({
            showScheduledJobInputModal: false,
            editingJob: null
        });
    },

    handleEditJobSubmit(job) {
        JobActions.updateJob(job);
        this.closeEditJobModal();
    },

    onJobTypeChange(jobType) {
        this.setState({jobType: jobType})
    },

    render: function () {
        const clearLeftFix = {
            clear: 'left',
        };
        return (
            <div>
                <div className="pull-left">
                    <JobTypeDropdown onJobTypeChange={this.onJobTypeChange} />
                    <RepositoryDropDown />
                </div>
                <div className="pull-right">
                    <Button bsStyle="primary" onClick={this.openCreateJobModal}>
                        Create Job
                    </Button>
                </div>

                <div style={clearLeftFix}></div>
                
                <ScheduledJobInputModal 
                    show={this.state.showScheduledJobInputModal}
                    job={this.state.editingJob}
                    onClose={this.state.editingJob ? this.closeEditJobModal : this.closeCreateJobModal}
                    onSubmit={this.state.editingJob ? this.handleEditJobSubmit : this.handleCreateJobSubmit} 
                />

                <AltContainer store={JobStore} className="mtl mbl" >
                    <JobsView jobType={this.state.jobType} openEditJobModal={this.openEditJobModal} />
                </AltContainer>
            </div>
        );
    },
});

export default withRouter(JobsPage);
