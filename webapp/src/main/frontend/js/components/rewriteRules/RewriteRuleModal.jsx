import React from "react";
import createReactClass from 'create-react-class';
import {FormattedMessage, injectIntl} from "react-intl";
import {Button, Checkbox, ControlLabel, FormControl, FormGroup, Modal} from "react-bootstrap";
import SearchableLocaleDropdown from "./SearchableLocaleDropdown";
import SearchableRepositoryDropdown from "./SearchableRepositoryDropdown";

let RewriteRuleModal = createReactClass({
    displayName: 'RewriteRuleModal',

    getInitialState() {
        return this.getDefaultState();
    },

    getDefaultState() {
        return {
            rewriteFrom: "",
            rewriteTo: "",
            repositoryId: "",
            localeId: "",
            enabled: true,
        };
    },

    componentDidUpdate(prevProps) {
        if (this.props.show && !prevProps.show) {
            if (this.props.rule) {
                this.setState({
                    rewriteFrom: this.props.rule.rewriteFrom || "",
                    rewriteTo: this.props.rule.rewriteTo || "",
                    repositoryId: this.props.rule.repositoryId || "",
                    localeId: this.props.rule.localeId || "",
                    enabled: this.props.rule.enabled,
                });
            } else {
                this.setState(this.getDefaultState());
            }
        }
    },

    handleSubmit(e) {
        e.preventDefault();
        const body = {
            rewriteFrom: this.state.rewriteFrom.trim(),
            rewriteTo: this.state.rewriteTo.trim(),
            enabled: this.state.enabled,
            localeId: this.state.localeId ? parseInt(this.state.localeId, 10) : null,
            repositoryId: this.props.scope !== "global" && this.state.repositoryId
                ? parseInt(this.state.repositoryId, 10)
                : null,
        };
        this.props.onSubmit(body);
    },

    handleChange(field, e) {
        this.setState({[field]: e.target.value});
    },

    handleEnabledChange(e) {
        this.setState({enabled: e.target.checked});
    },

    handleClose() {
        this.setState(this.getDefaultState());
        this.props.onClose();
    },

    isValid() {
        if (!this.state.rewriteFrom.trim()) return false;
        if (!this.state.rewriteTo.trim()) return false;
        if (this.props.scope === "repository" && !this.state.repositoryId) return false;
        if (!this.state.localeId) return false;
        return true;
    },

    render() {
        const {show, rule, scope, isSubmitting, error, intl} = this.props;
        const isEditing = rule !== null;

        return (
            <Modal show={show} onHide={this.handleClose}>
                <Modal.Header closeButton>
                    <Modal.Title>
                        {isEditing
                            ? <FormattedMessage id="rewriteRules.modal.title.edit"/>
                            : <FormattedMessage id="rewriteRules.modal.title.create"/>
                        }
                    </Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    {error && (
                        <div className="alert alert-danger">
                            {error}
                        </div>
                    )}
                    <form onSubmit={this.handleSubmit}>
                        <FormGroup>
                            <ControlLabel htmlFor="rewriteRuleFrom"><FormattedMessage id="rewriteRules.modal.rewriteFrom.label"/></ControlLabel>
                            <FormControl
                                id="rewriteRuleFrom"
                                type="text"
                                placeholder={intl.formatMessage({id: "rewriteRules.modal.rewriteFrom.placeholder"})}
                                value={this.state.rewriteFrom}
                                onChange={(e) => this.handleChange("rewriteFrom", e)}
                            />
                            <p className="help-block"><FormattedMessage id="rewriteRules.modal.rewriteFrom.help"/></p>
                        </FormGroup>

                        <FormGroup>
                            <ControlLabel htmlFor="rewriteRuleTo"><FormattedMessage id="rewriteRules.modal.rewriteTo.label"/></ControlLabel>
                            <FormControl
                                id="rewriteRuleTo"
                                type="text"
                                placeholder={intl.formatMessage({id: "rewriteRules.modal.rewriteTo.placeholder"})}
                                value={this.state.rewriteTo}
                                onChange={(e) => this.handleChange("rewriteTo", e)}
                            />
                            <p className="help-block"><FormattedMessage id="rewriteRules.modal.rewriteTo.help"/></p>
                        </FormGroup>

                        {scope !== "global" && (
                            <FormGroup>
                                <ControlLabel htmlFor="rewriteRuleRepository"><FormattedMessage id="rewriteRules.modal.repository.label"/></ControlLabel>
                                <SearchableRepositoryDropdown
                                    id="rewriteRuleRepository"
                                    repositoryOptions={this.props.repositories || []}
                                    selectedRepositoryId={this.state.repositoryId}
                                    onSelect={(repo) => this.setState({repositoryId: repo ? String(repo.id) : ""})}
                                    showNoneOption={scope === "all"}
                                />
                            </FormGroup>
                        )}

                        <FormGroup>
                            <ControlLabel htmlFor="rewriteRuleLocale"><FormattedMessage id="rewriteRules.modal.locale.label"/></ControlLabel>
                            <SearchableLocaleDropdown
                                id="rewriteRuleLocale"
                                localeOptions={this.props.locales || []}
                                selectedLocaleId={this.state.localeId}
                                onSelect={(locale) => this.setState({localeId: locale ? String(locale.id) : ""})}
                            />
                        </FormGroup>

                        <FormGroup>
                            <Checkbox
                                id="rewriteRuleEnabled"
                                checked={this.state.enabled}
                                onChange={this.handleEnabledChange}
                            >
                                <FormattedMessage id="rewriteRules.modal.enabled"/>
                            </Checkbox>
                        </FormGroup>
                        <button type="submit" style={{display: "none"}}/>
                    </form>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={this.handleClose}><FormattedMessage id="rewriteRules.modal.cancel"/></Button>
                    <Button
                        bsStyle="primary"
                        disabled={!this.isValid() || isSubmitting}
                        onClick={this.handleSubmit}
                    >
                        {isSubmitting
                            ? <FormattedMessage id="rewriteRules.modal.saving"/>
                            : (isEditing
                                ? <FormattedMessage id="rewriteRules.modal.update"/>
                                : <FormattedMessage id="rewriteRules.modal.create"/>)
                        }
                    </Button>
                </Modal.Footer>
            </Modal>
        );
    },
});

export default injectIntl(RewriteRuleModal);
