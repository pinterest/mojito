import _ from "lodash";
import React from "react";
import createReactClass from 'create-react-class';
import {FormattedMessage, injectIntl} from "react-intl";
import {DropdownButton, MenuItem} from "react-bootstrap";
import FluxyMixin from "alt-mixins/FluxyMixin";
import RepositoriesStore from "../../stores/RepositoryStore";
import RewriteRuleActions from "../../actions/rewriteRules/RewriteRuleActions";
import RewriteRuleStore from "../../stores/rewriteRules/RewriteRuleStore";

let RepositoryDropDown = createReactClass({
    displayName: 'RepositoryDropDown',
    mixins: [FluxyMixin],

    statics: {
        storeListeners: {
            "onRepositoriesFetched": RepositoriesStore,
            "onRepositoryIdChanged": RewriteRuleStore
        }
    },

    /**
     * Currently there is no way to prevent the dropdown to close on select
     * unless using a trick based on this attribute.
     *
     * Action that shouldn't close the dropdown can set this attribute to 'true'
     * This will prevent onDropdownToggle to actually close the dropdown.
     * Subsequent calls to onDropdownToggle will behave normally.
     */
    forceDropdownOpen: false,

    onRepositoriesFetched() {
       this.updateComponent();
    },

    onRepositoryIdChanged() {
        this.updateComponent();
    },

    updateComponent() {
        this.setState({
            "repositories": RepositoriesStore.getState().repositories.slice(),
            "selectedRepoIds": RewriteRuleStore.getState().repoIds.slice().sort()
        });
    },

    getInitialState: function () {
        return {
            /** @type Number[] */
            "repositories": RepositoriesStore.getState().repositories.slice(),
            /** @type Number[] */
            "selectedRepoIds": RewriteRuleStore.getState().repoIds.slice().sort(),
            /** @type boolean */
            "isDropdownOpened": false
        };
    },

    /**
     * Get list of repositories (with selected state) sorted by their display name
     *
     * @return {{id: number, name: string, selected: boolean}[]}}
     */
    getSortedRepositories() {
        return this.state.repositories
            .map((repository) => {
                return {
                    "id": repository.id,
                    "name": repository.name,
                    "selected": this.state.selectedRepoIds.indexOf(repository.id) > -1
                }
            }).sort((a, b) => a.name.localeCompare(b.name));
    },

    /**
     * On dropdown selected event, add or remove the target repository from the
     * selected repository list base on its previous state (selected or not).
     *
     * @param repository the repository that was selected
     */
    onRepositorySelected(repository, event) {
        this.forceDropdownOpen = true;
        let id = repository.id;
        let newSelectedRepoIds = null;
        if (event.shiftKey) {
            newSelectedRepoIds = [id];
        } else {
            newSelectedRepoIds = this.state.selectedRepoIds.slice();

            if (repository.selected) {
                _.pull(newSelectedRepoIds, id);
            } else {
                newSelectedRepoIds.push(id);
            }
        }
        this.setSelectedRepoIds(newSelectedRepoIds);
    },

    setSelectedRepoIds(newSelectedRepoIds) {
        RewriteRuleActions.setSelectedRepoIds(newSelectedRepoIds);
    },

    /**
     * Gets the text to display on the button.
     *
     * if 1 repository selected the named is shown, else the number of selected repositories is displayed (with proper i18n support)
     *
     * @returns {string} text to display on the button
     */
    getButtonText() {
        let label = '';
        let numberOfSelectedRepositories = this.state.selectedRepoIds.length;
        if (numberOfSelectedRepositories === 1) {
            let repoId = this.state.selectedRepoIds[0];
            let repo = this.getRepositoryById(repoId);
            label = repo ? repo.name : this.props.intl.formatMessage({"id": "rewriteRules.loading"});
        } else {
            label = this.props.intl.formatMessage({"id": "search.repository.btn.text"}, {"numberOfSelectedRepositories": numberOfSelectedRepositories});
        }
        return label;
    },

    /**
     * Gets a repository by id from the state.
     *
     * @param repoId the repository id
     */
    getRepositoryById(repoId) {
        return _.find(this.state.repositories, {"id": repoId});
    },

    /**
     * Here we handle the logic to keep the dropdown open because it is not
     * supported by default react-bootstrap component.
     *
     * "forceDropdownOpen" can be set in any function that wants to prevent the
     * the dropdown to close.
     *
     * @param newOpenState
     */
    onDropdownToggle(newOpenState){

        if (this.forceDropdownOpen) {
            this.forceDropdownOpen = false;
            this.setState({"isDropdownOpened": true});
        } else {
            this.setState({"isDropdownOpened": newOpenState});
        }
    },

    getRepositoryIdsFromState() {
        return this.state.repositories.map(repository => repository.id);
    },

    onSelectAll() {
        this.forceDropdownOpen = true;
        this.setSelectedRepoIds(this.getRepositoryIdsFromState());
    },

    isAllActive() {
        return this.state.selectedRepoIds.length > 0 && this.state.selectedRepoIds.length === this.getRepositoryIdsFromState().length;
    },

     onSelectNone() {
        this.forceDropdownOpen = true;
        this.setSelectedRepoIds([]);
    },

    isNoneActive() {
        return this.state.selectedRepoIds.length === 0;
    },

    /**
     * Renders the locale menu item list.
     *
     * @returns XML
     */
    renderRepositories() {
        return this.getSortedRepositories().map(
            (repository) =>
                <MenuItem key={"RewriteRuleRepositoryDropdown." + repository.name} eventKey={repository}
                          active={repository.selected} onSelect={this.onRepositorySelected}>{repository.name}
                </MenuItem>
        );
    },

    render() {

        return (
            <span className="mlm repository-dropdown">
                <DropdownButton id="RewriteRuleRepositoryDropdown" title={this.getButtonText()}
                                onToggle={this.onDropdownToggle} open={this.state.isDropdownOpened}>
                    <MenuItem id="RewriteRuleRepositoryDropdown.selectAll" active={this.isAllActive()} onSelect={this.onSelectAll}>
                        <FormattedMessage id="search.repository.selectAll"/>
                    </MenuItem>
                    <MenuItem id ="RewriteRuleRepositoryDropdown.selectNone" active={this.isNoneActive()} onSelect={this.onSelectNone}>
                        <FormattedMessage id="search.repository.selectNone"/>
                    </MenuItem>
                    <MenuItem id="RewriteRuleRepositoryDropdown.divider" divider/>
                    {this.renderRepositories()}
                </DropdownButton>
                </span>
        );
    },
});

export default injectIntl(RepositoryDropDown);
