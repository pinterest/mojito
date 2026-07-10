import alt from "../../alt.js";
import RewriteRuleActions from "../../actions/rewriteRules/RewriteRuleActions.js";
import RewriteRuleDataSource from "../../actions/rewriteRules/RewriteRuleDataSource.js";

class RewriteRuleStore {

    constructor() {
        this.rules = [];
        this.totalElements = 0;
        this.page = 0;
        this.pageSize = 10;
        this.scope = "all";
        this.isMutating = false;
        this.error = null;
        this.isLoading = false;
        this.repoIds = [];
        this.localeIds = [];
        this.rewriteFrom = "";
        this.sortField = "rewriteFrom";
        this.sortDirection = "asc";
        this.bindActions(RewriteRuleActions);
        this.registerAsync(RewriteRuleDataSource);
    }

    getAllRewriteRules() {
        if (this.localeIds.length === 0 || (this.scope === 'repository' && this.repoIds.length === 0)) {
            this.rules = [];
        } else {
            this.isLoading = true;
            this.getInstance().getAllRewriteRules();
        }
    }

    getAllRewriteRulesSuccess(response) {
        this.rules = response.content || [];
        this.totalElements = response.totalElements || 0;
        this.isLoading = false;
        this.setError(null);
    }

    getAllRewriteRulesError(error) {
        this.isLoading = false;
        this.setError(error);
    }

    createRewriteRule(body) {
        this.isMutating = true;
        this.getInstance().createRewriteRule(body);
    }

    createRewriteRuleSuccess() {
        this.setError(null);
        this.getAllRewriteRules();
        this.isMutating = false;
    }

    createRewriteRuleError(error) {
        this.setError(error);
    }

    updateRewriteRule({ id, body }) {
        this.isMutating = true;
        this.getInstance().updateRewriteRule(id, body);
    }

    updateRewriteRuleSuccess() {
        this.setError(null);
        this.getAllRewriteRules();
        this.isMutating = false;
    }

    updateRewriteRuleError(error) {
        this.setError(error);
    }

    deleteRewriteRule(id) {
        this.isMutating = true;
        this.getInstance().deleteRewriteRule(id);
    }

    deleteRewriteRuleSuccess() {
        this.setError(null);
        this.getAllRewriteRules();
        this.isMutating = false;
    }

    deleteRewriteRuleError(error) {
        this.setError(error);
    }

    enableRewriteRule(id) {
        this.isMutating = true;
        this.getInstance().enableRewriteRule(id);
    }

    enableRewriteRuleSuccess(rule) {
        this.rules = this.rules.map(r => r.id === rule.id ? rule : r);
        this.setError(null);
        this.isMutating = false;
    }

    enableRewriteRuleError(error) {
        this.setError(error);
    }

    disableRewriteRule(id) {
        this.isMutating = true;
        this.getInstance().disableRewriteRule(id);
    }

    disableRewriteRuleSuccess(rule) {
        this.rules = this.rules.map(r => r.id === rule.id ? rule : r);
        this.setError(null);
        this.isMutating = false;
    }

    disableRewriteRuleError(error) {
        this.setError(error);
    }

    setScope(scope) {
        this.scope = scope;
        this.page = 0;
    }

    setPage({ page, pageSize }) {
        this.page = page;
        this.pageSize = pageSize;
        this.getAllRewriteRules();
    }

    setSort({ field, direction }) {
        this.sortField = field;
        this.sortDirection = direction;
        this.page = 0;
        this.getAllRewriteRules();
    }

    setError(error) {
        if (error && error.response && typeof error.response.json === 'function') {
            error.response.json().then(data => {
                this.error = (data && data.message) || error.message;
                this.isMutating = false;
                this.emitChange();
            }).catch(() => {
                this.error = error.message;
                this.isMutating = false;
                this.emitChange();
            });
        } else {
            this.error = error ? (error.message || String(error)) : null;
            this.isMutating = false;
        }
    }

    setSelectedRepoIds(repoIds) {
        this.repoIds = repoIds;
        this.page = 0;
    }

    setSelectedLocaleIds(localeIds) {
        this.localeIds = localeIds;
        this.page = 0;
        this.getAllRewriteRules();
    }

    setRewriteFrom(rewriteFrom) {
        this.rewriteFrom = rewriteFrom;
        this.page = 0;
        this.getAllRewriteRules();
    }

    resetError() {
        this.error = null;
    }
}

export default alt.createStore(RewriteRuleStore, 'RewriteRuleStore');
