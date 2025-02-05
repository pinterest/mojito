import React from "react";
import createReactClass from 'create-react-class';
import {FormattedMessage, injectIntl} from "react-intl";
import {DropdownButton, MenuItem} from "react-bootstrap";
import FluxyMixin from "alt-mixins/FluxyMixin";
import JobStore from "../../stores/jobs/JobStore";
import JobActions from "../../actions/jobs/JobActions";

let RepositoryDropDown = createReactClass({
    displayName: 'RepositoryDropDown',
    mixins: [FluxyMixin],

    statics: {
        storeListeners: {
            "getAllJobsSuccess": JobStore
        }
    },
    forceDropdownOpen: false,

    getAllJobsSuccess(store) {
        if(!this.loaded) {
            this.setState({repositories: store.jobs.map(j => j.repository)})
            this.loaded = true;
        }
    },

    onDropdownToggle(newOpenState){
        if (this.forceDropdownOpen) {
            this.forceDropdownOpen = false;
            this.setState({"isDropdownOpenned": true});
        } else {
            this.setState({"isDropdownOpenned": newOpenState});
        }
    },

    getInitialState: function () {
        return {
            "repositories": [],
            "selectedRepositories": []
        };
    },

    onRepositorySelected(repository, event) {
        this.forceDropdownOpen = true;

        let selectedRepos = this.state.selectedRepositories;

        if(selectedRepos.includes(repository)) {
            selectedRepos = selectedRepos.filter(repo => repo !== repository);
        } else {
            selectedRepos.push(repository);
        }

        this.setState({selectedRepositories: selectedRepos});
        JobActions.setJobFilter(selectedRepos);
    },

    onSelectAll() {
        this.forceDropdownOpen = true;
        this.setState({selectedRepositories: []});
        JobActions.setJobFilter([]);
    },

    isAllActive() {
        return this.state.selectedRepositories.length === 0;
    },


    renderRepositories() {
        return this.state.repositories.map(
            (repository) =>
                <MenuItem key={"WorkbenchRepositoryDropdown." + repository} eventKey={repository} active={this.state.selectedRepositories.includes(repository)} onSelect={this.onRepositorySelected}>{repository}</MenuItem>
        );
    },


    /**
     * @return {JSX}
     */
    render() {

        return (
            <span className="mlm repository-dropdown">
                <DropdownButton id="WorkbenchRepositoryDropdown" title={"Repository"} onToggle={this.onDropdownToggle} open={this.state.isDropdownOpenned}>
                    <MenuItem id="WorkbenchRepositoryDropdown.selectAll" active={this.isAllActive()} onSelect={this.onSelectAll}><FormattedMessage id="search.repository.selectAll"/></MenuItem>

                    <MenuItem id="WorkbenchRepositoryDropdown.divider" divider/>

                    {this.renderRepositories()}
                </DropdownButton>
                </span>
        );
    },
});

export default injectIntl(RepositoryDropDown);
