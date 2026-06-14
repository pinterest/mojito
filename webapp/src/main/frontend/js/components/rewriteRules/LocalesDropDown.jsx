import _ from "lodash";
import React from "react";
import createReactClass from 'create-react-class';
import PropTypes from "prop-types";
import {FormattedMessage, injectIntl} from 'react-intl';
import {DropdownButton, MenuItem, OverlayTrigger, Tooltip} from "react-bootstrap";
import FluxyMixin from "alt-mixins/FluxyMixin";

import RepositoryStore from "../../stores/RepositoryStore";
import RewriteRuleStore from "../../stores/rewriteRules/RewriteRuleStore";
import RewriteRuleActions from "../../actions/rewriteRules/RewriteRuleActions";
import Locales from "../../utils/Locales";

let LocalesDropDown = createReactClass({
    displayName: 'LocalesDropDown',
    propTypes: {
        scope: PropTypes.oneOf(['all', 'global', 'repository']).isRequired
    },
    mixins: [FluxyMixin],

    statics: {
        storeListeners: {
            "onRepositoriesFetched": RepositoryStore,
            "onRewriteRuleStoreChanged": RewriteRuleStore
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

    /**
     * Handler for when RepositoryStore is updated
     */
    onRepositoriesFetched() {
        this.updateComponent();
    },

    areArraysEqual(array1, array2) {
        return array1.length === array2.length
                && array1.every((value, index) => value === array2[index]);
    },

    /**
     * Handler for when RewriteRuleStore is updated.
     * Only re-sync locale selection if available locales, repoIds, or scope changed,
     * to avoid an infinite loop when locale selection itself triggers a store update.
     */
    onRewriteRuleStoreChanged() {
        const previousLocales = this.state.locales;
        this.updateComponent();
        if (!_.isEqual(previousLocales, this.state.locales)
                || !this.areArraysEqual(this.state.repoIds, RewriteRuleStore.getState().repoIds)
                || this.state.scope !== RewriteRuleStore.getState().scope) {
            setTimeout(() => this.setSelectedLocaleIds(this.state.selectedLocaleIds), 0);
        }
        this.setState({
            repoIds: RewriteRuleStore.getState().repoIds,
            scope: RewriteRuleStore.getState().scope
        });
    },

    /**
     * State the state based on the stores and sync data with the multiselect component
     */
    updateComponent() {
        const locales = this.getSortedLocalesFromStore();
        this.setState({
            locales,
            selectedLocaleIds: this.getSortedSelectedLocaleIdsFromStore(locales)
        });
    },

    /**
     * @return {{locales: object[], selectedLocaleIds: number[], isDropdownOpened: boolean}}
     */
    getInitialState() {
        return {
            "locales": [],
            "selectedLocaleIds": [],
            "isDropdownOpened": false,
            "repoIds": [],
            "scope": RewriteRuleStore.getState().scope,
        };
    },

    /**
     * Gets sorted bcp47tags from stores.
     *
     * Sort is important to ensure later array comparison in the component will
     * work as expected.
     *
     * @returns {string[]}
     */
    getSortedLocalesFromStore() {
        let repositoryIds = RewriteRuleStore.getState().repoIds;
        if (this.props.scope !== 'repository') {
            repositoryIds = RepositoryStore.getState().repositories.map(repository => repository.id);
        }
        return RepositoryStore.getAllLocalesForRepositoryIds(repositoryIds)
                .sort((a, b) => a.bcp47Tag.localeCompare(b.bcp47Tag));
    },

    /**
     * Gets sorted selected bcp47tags from stores.
     *
     * Sort is important to ensure later array comparison in the component will
     * work as expected.
     *
     * @returns {T[]}
     */
    getSortedSelectedLocaleIdsFromStore(locales) {
        const allLocaleIds = locales.map(locale => locale.id);
        return RewriteRuleStore.getState().localeIds.filter(localeId => allLocaleIds.indexOf(localeId) > -1).sort();
    },

    /**
     * Get list of locales (with selected state) sorted by their display name
     *
     * @return {{bcp47Tag: string, displayName: string, selected: boolean}[]}}
     */
    getSortedLocales() {
        return this.state.locales
            .map((locale) => {
                return {
                    "id": locale.id,
                    "bcp47Tag": locale.bcp47Tag,
                    "displayName": Locales.getDisplayName(locale.bcp47Tag),
                    "selected": this.state.selectedLocaleIds.indexOf(locale.id) > -1
                }
            }).sort((a, b) => a.displayName.localeCompare(b.displayName));
    },

    /**
     * On dropdown selected event, add or remove the target locale from the
     * selected locale list base on its previous state (selected or not).
     *
     * @param locale the locale that was selected
     */
    onLocaleSelected(locale, event) {
        this.forceDropdownOpen = true;

        let localeId = locale.id;

        let newSelectedLocaleIds = [];

        if (event.shiftKey) {
            newSelectedLocaleIds = [localeId];
        } else {
            newSelectedLocaleIds =  this.state.selectedLocaleIds.slice();

            if (locale.selected) {
                _.pull(newSelectedLocaleIds, localeId);
            } else {
                newSelectedLocaleIds.push(localeId);
            }
        }

        this.setSelectedLocaleIds(newSelectedLocaleIds);
    },

    /**
     * Trigger the searchParamsChanged action for a given list of selected
     * bcp47 tags.
     *
     * @param newSelectedLocaleIds
     */
    setSelectedLocaleIds(newSelectedLocaleIds) {
        RewriteRuleActions.setSelectedLocaleIds(newSelectedLocaleIds);
    },

    /**
     * Gets the text to display on the button.
     *
     * if 1 locale selected the named is shown, else the number of selected locale is displayed (with proper i18n support)
     *
     * @returns {string} text to display on the button
     */
    getButtonText() {

        let label = '';

        let numberOfSelectedLocales = this.state.selectedLocaleIds.length;

        if (numberOfSelectedLocales === 1 && this.state.locales.length > 0) {
            const localeId = this.state.selectedLocaleIds[0];
            const locale = this.state.locales.find(locale => locale.id === localeId);
            if (locale) {
                label = Locales.getDisplayName(locale.bcp47Tag);
            } else {
                label = this.props.intl.formatMessage({id: "search.locale.btn.text"}, {'numberOfSelectedLocales': numberOfSelectedLocales});
            }
        } else {
            label = this.props.intl.formatMessage({id: "search.locale.btn.text"}, {'numberOfSelectedLocales': numberOfSelectedLocales});
        }

        return label;
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

    /**
     * Selects all locales.
     */
    onSelectAll() {
        this.forceDropdownOpen = true;
        this.setSelectedLocaleIds(this.state.locales.map(locale => locale.id));
    },

    /**
     * Clear all selected locales.
     */
    onSelectNone() {
        this.forceDropdownOpen = true;
        this.setSelectedLocaleIds([]);
    },

    /**
     * Indicates if the select all menu item should be active.
     *
     * @returns {boolean}
     */
    isAllActive() {
        return this.state.selectedLocaleIds.length > 0 && this.state.selectedLocaleIds.length === this.state.locales.length;
    },

    /**
     * Indicates if the clear all menu item should be active.
     *
     * @returns {boolean}
     */
    isNoneActive() {
        return this.state.selectedLocaleIds.length === 0;
    },

    /**
     * Whether there is no data for the dropdown
     *
     * @returns {boolean}
     */
    hasLocaleData() {
        return this.state.locales.length > 0;
    },

    /**
     * Renders the locale menu item list.
     *
     * @returns {XML}
     */
    renderLocales() {
        return this.getSortedLocales().map((locale) => (
            <MenuItem
                key={"RewriteRule.LocaleDropdown." + locale.displayName}
                eventKey={locale}
                active={locale.selected}
                onSelect={this.onLocaleSelected}
            >
                {locale.displayName}
            </MenuItem>
        ));
    },

    wrapWithTooltipIfEmpty(wrappedComponent, hasLocaleData) {
        if (hasLocaleData) {
            return wrappedComponent;
        }

        return (
            <OverlayTrigger
                overlay={
                    <Tooltip id="locale-dropdown-tooltip">
                        <FormattedMessage id="rewriteRules.locale.selectRepositoryFirst"/>
                    </Tooltip>
                }
            >
                {wrappedComponent}
            </OverlayTrigger>
        );
    },

    /**
     * @return {JSX}
     */
    render() {
        const hasLocaleData = this.hasLocaleData();
        return (
            <span className="mlm locale-dropdown">
                    {
                        this.wrapWithTooltipIfEmpty(
                            <DropdownButton id="RewriteRuleLocaleDropdown" disabled={!hasLocaleData}
                                            title={this.getButtonText()} onToggle={this.onDropdownToggle}
                                            open={this.state.isDropdownOpened}>
                                <MenuItem active={this.isAllActive()} onSelect={this.onSelectAll}>
                                    <FormattedMessage id="search.locale.selectAll"/>
                                </MenuItem>
                                <MenuItem active={this.isNoneActive()} onSelect={this.onSelectNone}>
                                    <FormattedMessage id="search.locale.selectNone"/>
                                </MenuItem>
                                <MenuItem divider/>
                                {this.renderLocales()}
                            </DropdownButton>, hasLocaleData)}
                </span>
        );

    },
});

export default injectIntl(LocalesDropDown);
