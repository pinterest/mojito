import FluxyMixin from "alt-mixins/FluxyMixin";
import React from "react";
import createReactClass from 'create-react-class';
import {FormattedMessage, injectIntl} from "react-intl";
import {DropdownButton, FormGroup, FormControl, InputGroup, MenuItem, Button, Glyphicon} from "react-bootstrap";
import RewriteRuleActions from "../../actions/rewriteRules/RewriteRuleActions";
import RewriteRuleStore from "../../stores/rewriteRules/RewriteRuleStore";
import SearchParamsStore from "../../stores/workbench/SearchParamsStore";

let SearchText = createReactClass({
    displayName: 'SearchText',
    mixins: [FluxyMixin],

    statics: {
        storeListeners: {
            "onRewriteRuleStoreChanged": RewriteRuleStore
        }
    },

    /**
     *
     * @return {{searchText: (*|string), isSpinnerShown: (*|boolean|Boolean)}}
     */
    getInitialState() {
        return {
            /** @type {string} */
            "searchText": "",

            /** @type {string} */
            "searchType": RewriteRuleStore.getState().searchType,

            /** @type {Boolean} */
            "isSpinnerShown": RewriteRuleStore.getState().isLoading
        };
    },

    onRewriteRuleStoreChanged() {
        this.setState({
            "isSpinnerShown": RewriteRuleStore.getState().isLoading,
            "searchType": RewriteRuleStore.getState().searchType
        });
    },

    onSearchTypeSelected(searchType) {
        if (searchType !== this.state.searchType) {
            RewriteRuleActions.setSearchType(searchType.toUpperCase());
        }
    },

    onKeyDownOnSearchText(e) {
        if (e.key === 'Enter') {
            this.callSearchParamChanged();
        }
    },

    onSearchButtonClicked() {
        this.callSearchParamChanged();
    },

    callSearchParamChanged() {
        RewriteRuleActions.setRewriteFrom(this.state.searchText.trim());
    },

    getMessageForSearchType(searchType) {
        const lowerCaseSearchType = searchType.toLowerCase();
        switch (lowerCaseSearchType) {
            case SearchParamsStore.SEARCH_TYPES.EXACT:
                return this.props.intl.formatMessage({id: "search.filter.exact"});
            case SearchParamsStore.SEARCH_TYPES.CONTAINS:
                return this.props.intl.formatMessage({id: "search.filter.contains"});
            case SearchParamsStore.SEARCH_TYPES.ILIKE:
                return this.props.intl.formatMessage({id: "search.filter.ilike"});
        }
    },

    renderSearchTypeMenuItem(searchType) {
        return (
            <MenuItem eventKey={searchType} active={this.state.searchType === searchType.toUpperCase()}
                      onSelect={this.onSearchTypeSelected}>
                {this.getMessageForSearchType(searchType)}
            </MenuItem>
        );
    },

    renderDropdown() {
        return (
            <DropdownButton id="search-type-dropdown" title={this.getMessageForSearchType(this.state.searchType)}>
                <MenuItem header><FormattedMessage id="search.filter.searchType"/></MenuItem>
                {this.renderSearchTypeMenuItem(SearchParamsStore.SEARCH_TYPES.EXACT)}
                {this.renderSearchTypeMenuItem(SearchParamsStore.SEARCH_TYPES.CONTAINS)}
                {this.renderSearchTypeMenuItem(SearchParamsStore.SEARCH_TYPES.ILIKE)}
            </DropdownButton>
        );
    },

    renderSearchButton() {
        return (
            <Button onClick={this.onSearchButtonClicked}>
                <Glyphicon glyph='glyphicon glyphicon-search'/>
            </Button>
        );
    },

    /**
     *
     * @param {SyntheticEvent} event
     */
    searchTextOnChange(event) {
        this.setState({
            "searchText": event.target.value
        });
    },

    render: function () {
        return (
            <div className="col-xs-6 search-text">
                <FormGroup>
                    <InputGroup>
                        <InputGroup.Button>{this.renderDropdown()}</InputGroup.Button>
                        <FormControl id="RewriteRuleSearchText"
                                     type='text' value={this.state.searchText}
                                     onChange={this.searchTextOnChange}
                                     placeholder={this.props.intl.formatMessage({ id: "rewriteRules.search.placeholder" })}
                                     onKeyDown={this.onKeyDownOnSearchText}/>
                        <InputGroup>
                            {this.state.isSpinnerShown ? (<span className="glyphicon glyphicon-refresh spinning" />) : ""}
                        </InputGroup>
                        <InputGroup.Button>{this.renderSearchButton()}</InputGroup.Button>
                    </InputGroup>
                </FormGroup>
            </div>
        );
    },
});

export default injectIntl(SearchText);
