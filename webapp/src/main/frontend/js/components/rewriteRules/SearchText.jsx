import FluxyMixin from "alt-mixins/FluxyMixin";
import React from "react";
import createReactClass from 'create-react-class';
import {injectIntl} from "react-intl";
import {FormGroup, FormControl, InputGroup, Button, Glyphicon} from "react-bootstrap";
import RewriteRuleActions from "../../actions/rewriteRules/RewriteRuleActions";
import RewriteRuleStore from "../../stores/rewriteRules/RewriteRuleStore";

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

            /** @type {Boolean} */
            "isSpinnerShown": RewriteRuleStore.getState().isLoading
        };
    },

    onRewriteRuleStoreChanged() {
        this.setState({
            "isSpinnerShown": RewriteRuleStore.getState().isLoading
        });
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
