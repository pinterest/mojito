import React from "react";
import createReactClass from 'create-react-class';
import {withRouter} from "react-router";
import {FormattedMessage, injectIntl} from "react-intl";
import {Button, ButtonGroup, DropdownButton, MenuItem, Label, Table, Glyphicon, Modal} from "react-bootstrap";
import RepositoryDropDown from "./RepositoryDropDown";
import RewriteRuleStore from "../../stores/rewriteRules/RewriteRuleStore";
import RewriteRuleActions from "../../actions/rewriteRules/RewriteRuleActions";
import RewriteRuleModal from "./RewriteRuleModal";
import LocalesDropDown from "../rewriteRules/LocalesDropDown";
import SearchText from "./SearchText";
import RepositoryStore from "../../stores/RepositoryStore";
import Locales from "../../utils/Locales.js";
import Paginator from "../widgets/Paginator";
import AuthorityService from "../../utils/AuthorityService";

let RewriteRulesPage = createReactClass({
    displayName: 'RewriteRulesPage',

    getInitialState() {
        return {
            rules: [],
            totalElements: 0,
            page: 0,
            pageSize: 10,
            scope: "all",
            sortField: "rewriteFrom",
            sortDirection: "asc",
            locales: [],
            repositories: [],
            isLoading: false,
            error: null,
            showModal: false,
            editingRule: null,
            prefillRule: null,
            isSubmitting: false,
            showDeleteConfirm: false,
            deletingRule: null,
        };
    },

    getAllLocales() {
        return RepositoryStore.getAllLocalesForRepositories(RepositoryStore.getState().repositories, false);
    },

    onRepositoryChange() {
        this.setState({
            repositories: RepositoryStore.getState().repositories,
            locales: this.getAllLocales(),
        });
    },

    componentDidMount() {
        this.storeListener = RewriteRuleStore.listen(this.onStoreChange);
        this.repositoryListener = RepositoryStore.listen(this.onRepositoryChange);
        RewriteRuleActions.getAllRewriteRules();
    },

    componentWillUnmount() {
        this.setState({ locales: this.getAllLocales(), repositories: RepositoryStore.getState().repositories });
        RewriteRuleStore.unlisten(this.storeListener);
        RepositoryStore.unlisten(this.repositoryListener);
    },

    onStoreChange(storeState) {
        const newState = {
            rules: storeState.rules,
            totalElements: storeState.totalElements,
            page: storeState.page,
            pageSize: storeState.pageSize,
            scope: storeState.scope,
            sortField: storeState.sortField,
            sortDirection: storeState.sortDirection,
            isLoading: storeState.isLoading,
            error: storeState.error,
        };

        // Handle submission results
        if (this.state.isSubmitting && !storeState.isMutating && !storeState.error) {
            newState.showModal = false;
            newState.editingRule = null;
            newState.prefillRule = null;
            newState.isSubmitting = false;
        } else if (this.state.isSubmitting && storeState.error) {
            newState.isSubmitting = false;
        }

        this.setState(newState);
    },

    handleScopeChange(scope) {
        RewriteRuleActions.setScope(scope);
    },

    handlePageChange(newPage) {
        RewriteRuleActions.setPage({page: newPage, pageSize: this.state.pageSize});
    },

    handleSort(field) {
        const isSameField = this.state.sortField === field;
        const nextDirection = isSameField && this.state.sortDirection === "asc" ? "desc" : "asc";
        RewriteRuleActions.setSort({field, direction: nextDirection});
    },

    renderSortIcon(field) {
        if (this.state.sortField !== field) {
            return null;
        }

        return (
            <Glyphicon
                className="mls"
                glyph={this.state.sortDirection === "asc" ? "triangle-top" : "triangle-bottom"}
            />
        );
    },

    renderSortableHeader(field, messageId, className) {
        return (
            <th className={className}>
                <Button bsStyle="link" className="table-sort-btn" onClick={() => this.handleSort(field)}>
                    <FormattedMessage id={messageId}/>
                    {this.renderSortIcon(field)}
                </Button>
            </th>
        );
    },

    handleCreate() {
        this.setState({showModal: true, editingRule: null, prefillRule: null});
    },

    handleCopy(rule) {
        this.setState({showModal: true, editingRule: null, prefillRule: rule});
    },

    handleEdit(rule) {
        this.setState({showModal: true, editingRule: rule, prefillRule: null});
    },

    handleModalClose() {
        this.setState({showModal: false, editingRule: null, prefillRule: null, isSubmitting: false});
        RewriteRuleActions.resetError();
    },

    handleModalSubmit(body) {
        this.setState({isSubmitting: true});
        if (this.state.editingRule) {
            RewriteRuleActions.updateRewriteRule({id: this.state.editingRule.id, body});
        } else {
            RewriteRuleActions.createRewriteRule(body);
        }
    },

    handleDelete(rule) {
        this.setState({showDeleteConfirm: true, deletingRule: rule});
    },

    confirmDelete() {
        if (this.state.deletingRule) {
            RewriteRuleActions.deleteRewriteRule(this.state.deletingRule.id);
        }
        this.setState({showDeleteConfirm: false, deletingRule: null});
    },

    cancelDelete() {
        this.setState({showDeleteConfirm: false, deletingRule: null});
    },

    handleToggleEnabled(rule) {
        if (rule.enabled) {
            RewriteRuleActions.disableRewriteRule(rule.id);
        } else {
            RewriteRuleActions.enableRewriteRule(rule.id);
        }
    },

    getRepositoryName(repositoryId) {
        const repositoryNames = this.state.repositories.filter(repository => repository.id === repositoryId)
            .map(repository => repository.name);
        if (repositoryNames.length > 0) {
            return repositoryNames[0];
        }
        return '';
    },

    getLocaleDisplayName(localeId) {
        const bcp47Tags = this.state.locales.filter(locale => locale.id === localeId).map(locale => locale.bcp47Tag);
        if (bcp47Tags.length > 0) {
            const bcp47Tag = bcp47Tags[0];
            return Locales.getDisplayName(bcp47Tag);
        }
        return '';
    },

    render() {
        if (!AuthorityService.canViewRewriteRules()) {
            return <h3 className="text-center mtl"><FormattedMessage id="rewriteRules.forbidden"/></h3>;
        }

        const {rules, scope, isLoading, showDeleteConfirm, deletingRule, page, pageSize, totalElements} = this.state;
        const totalPages = Math.ceil(totalElements / pageSize);

        const scopeTitle = this.props.intl.formatMessage({id: scope === "all" ? "rewriteRules.scope.all" : scope === "global" ? "rewriteRules.scope.global" : "rewriteRules.scope.repository"});

        return (
            <div>
                <div className="row">
                    <div className="pull-left mlm">
                        <DropdownButton
                            id="scope-dropdown"
                            title={scopeTitle}
                            onSelect={this.handleScopeChange}
                        >
                            <MenuItem eventKey="all" active={scope === "all"}><FormattedMessage id="rewriteRules.scope.all"/></MenuItem>
                            <MenuItem eventKey="global" active={scope === "global"}><FormattedMessage id="rewriteRules.scope.global"/></MenuItem>
                            <MenuItem eventKey="repository" active={scope === "repository"}><FormattedMessage id="rewriteRules.scope.repository"/></MenuItem>
                        </DropdownButton>
                        <span style={{display: scope === "repository" ? "inline-block" : "none"}}>
                        <RepositoryDropDown/>
                    </span>
                        <LocalesDropDown scope={scope}/>
                    </div>
                    <SearchText/>

                    <Button bsStyle="primary" className="pull-right mrm" onClick={this.handleCreate}>
                        <FormattedMessage id="rewriteRules.createRule"/>
                    </Button>
                </div>

                <div className="mtl mbl mlm mrm">
                    {isLoading && rules.length === 0 && (
                        <div className="text-center ptl mlm mrm">
                            <span className="glyphicon glyphicon-refresh glyphicon-refresh-animate"></span> <FormattedMessage id="rewriteRules.loading"/>
                        </div>
                    )}

                    {!isLoading && rules.length === 0 && (
                        <div className="text-center ptl mlm mrm">
                            <h4><FormattedMessage id="rewriteRules.noResults"/></h4>
                        </div>
                    )}

                    {rules.length > 0 && (
                        <>
                            <div className="pagination-toolbar mrm mtm mbm">
                                <div><FormattedMessage id="rewriteRules.totalElements" values={{totalElements}}/></div>
                                <DropdownButton
                                    id="rewrite-rules-page-size"
                                    title={this.props.intl.formatMessage({id: "rewriteRules.pageSize"}, {pageSize})}
                                >
                                    {[10, 20, 50, 100].map(size => (
                                        <MenuItem
                                            key={size}
                                            eventKey={size}
                                            active={size === pageSize}
                                            onSelect={(s) => RewriteRuleActions.setPage({page: 0, pageSize: s})}
                                        >
                                            {size}
                                        </MenuItem>
                                    ))}
                                </DropdownButton>
                                <Paginator
                                    currentPageNumber={page + 1}
                                    hasNextPage={page < totalPages - 1}
                                    onPreviousPageClicked={() => this.handlePageChange(page - 1)}
                                    onNextPageClicked={() => this.handlePageChange(page + 1)}
                                    disabled={isLoading}
                                    shown={true}
                                />
                            </div>
                            <Table className="rewrite-rules-table">
                                <thead>
                                <tr>
                                    {this.renderSortableHeader("rewriteFrom", "rewriteRules.table.rewriteFrom", "col-xs-3")}
                                    {this.renderSortableHeader("rewriteTo", "rewriteRules.table.rewriteTo", "col-xs-3")}
                                    <th className="col-xs-1"><FormattedMessage id="rewriteRules.table.status"/></th>
                                    {scope !== "global" && <th className="col-xs-1"><FormattedMessage id="rewriteRules.table.repository"/></th>}
                                    <th className="col-xs-1"><FormattedMessage id="rewriteRules.table.locale"/></th>
                                    <th className="col-xs-1"><FormattedMessage id="rewriteRules.table.createdBy"/></th>
                                    {this.renderSortableHeader("lastModifiedDate", "rewriteRules.table.lastModified", "col-xs-1")}
                                    <th className={scope === 'global' ? 'col-xs-2' : 'col-xs-1'}><FormattedMessage id="rewriteRules.table.actions"/></th>
                                </tr>
                                </thead>
                                <tbody>
                                {rules.map(rule => (
                                    <tr key={rule.id}>
                                        <td className="rewrite-rules-cell" title={rule.rewriteFrom}>{rule.rewriteFrom}</td>
                                        <td className="rewrite-rules-cell" title={rule.rewriteTo}>{rule.rewriteTo}</td>
                                        <td>
                                            {rule.enabled
                                                ? <Label bsStyle="success"><FormattedMessage id="rewriteRules.status.enabled"/></Label>
                                                : <Label bsStyle="default"><FormattedMessage id="rewriteRules.status.disabled"/></Label>
                                            }
                                        </td>
                                        {scope !== "global" && <td className="rewrite-rules-cell" title={this.getRepositoryName(rule.repositoryId)}>{this.getRepositoryName(rule.repositoryId)}</td>}
                                        <td className="rewrite-rules-cell" title={this.getLocaleDisplayName(rule.localeId)}>{this.getLocaleDisplayName(rule.localeId)}</td>
                                        <td className="rewrite-rules-cell" title={rule.createdByUserName}>{rule.createdByUserName}</td>
                                        <td className="rewrite-rules-cell">{rule.lastModifiedDate ? new Date(rule.lastModifiedDate).toLocaleDateString() : ""}</td>
                                        <td>
                                            <ButtonGroup bsSize="xsmall">
                                                <Button
                                                    bsStyle={rule.enabled ? "warning" : "success"}
                                                    onClick={() => this.handleToggleEnabled(rule)}
                                                >
                                                    {rule.enabled ? <FormattedMessage id="rewriteRules.action.disable"/> : <FormattedMessage id="rewriteRules.action.enable"/>}
                                                </Button>
                                                <Button onClick={() => this.handleEdit(rule)} title={this.props.intl.formatMessage({ id: "rewriteRules.action.edit" })}>
                                                    <Glyphicon glyph="pencil"/>
                                                </Button>
                                                <Button onClick={() => this.handleCopy(rule)} title={this.props.intl.formatMessage({ id: "rewriteRules.action.copy" })}>
                                                    <Glyphicon glyph="duplicate"/>
                                                </Button>
                                                <Button bsStyle="danger" onClick={() => this.handleDelete(rule)} title={this.props.intl.formatMessage({ id: "rewriteRules.action.delete" })}>
                                                    <Glyphicon glyph="trash"/>
                                                </Button>
                                            </ButtonGroup>
                                        </td>
                                    </tr>
                                ))}
                                </tbody>
                            </Table>
                        </>
                    )}
                </div>

                <RewriteRuleModal
                    show={this.state.showModal}
                    rule={this.state.editingRule}
                    prefillRule={this.state.prefillRule}
                    scope={scope}
                    locales={this.state.locales}
                    repositories={this.state.repositories}
                    error={this.state.error}
                    isSubmitting={this.state.isSubmitting}
                    onSubmit={this.handleModalSubmit}
                    onClose={this.handleModalClose}
                />

                <Modal show={!this.state.showModal && !!this.state.error} onHide={() => RewriteRuleActions.resetError()}>
                    <Modal.Header closeButton>
                        <Modal.Title><FormattedMessage id="rewriteRules.errorModal.title"/></Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        <div className="alert alert-danger">
                            {this.state.error}
                        </div>
                    </Modal.Body>
                    <Modal.Footer>
                        <Button onClick={() => RewriteRuleActions.resetError()}><FormattedMessage id="rewriteRules.errorModal.close"/></Button>
                    </Modal.Footer>
                </Modal>

                <Modal show={showDeleteConfirm} onHide={this.cancelDelete}>
                    <Modal.Header closeButton>
                        <Modal.Title><FormattedMessage id="rewriteRules.deleteModal.title"/></Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        {deletingRule && (
                            <p><FormattedMessage id="rewriteRules.deleteModal.confirm" values={{rewriteFrom: deletingRule.rewriteFrom, rewriteTo: deletingRule.rewriteTo}}/></p>
                        )}
                    </Modal.Body>
                    <Modal.Footer>
                        <Button onClick={this.cancelDelete}><FormattedMessage id="rewriteRules.deleteModal.cancel"/></Button>
                        <Button bsStyle="danger" onClick={this.confirmDelete}><FormattedMessage id="rewriteRules.deleteModal.delete"/></Button>
                    </Modal.Footer>
                </Modal>
            </div>
        );
    },
});

export default withRouter(injectIntl(RewriteRulesPage));
