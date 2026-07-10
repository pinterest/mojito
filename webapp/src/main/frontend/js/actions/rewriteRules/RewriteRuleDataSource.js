import RewriteRuleClient from "../../sdk/RewriteRuleClient.js";
import RewriteRuleActions from "./RewriteRuleActions.js";

const RewriteRuleDataSource = {
    getAllRewriteRules: {
        remote(state) {
            const params = {
                page: state.page,
                size: state.pageSize,
                scope: state.scope === 'all' ? null : state.scope,
                repositoryIds: state.scope === 'repository' ? state.repoIds : null,
                localeIds: state.localeIds,
                rewriteFrom: state.rewriteFrom,
                sort: `${state.sortField || 'rewriteFrom'},${state.sortDirection || 'asc'}`,
            };
            return RewriteRuleClient.getRewriteRules(params);
        },
        success: RewriteRuleActions.getAllRewriteRulesSuccess,
        error: RewriteRuleActions.getAllRewriteRulesError
    },

    createRewriteRule: {
        remote(state, body) {
            return RewriteRuleClient.createRewriteRule(body);
        },
        success: RewriteRuleActions.createRewriteRuleSuccess,
        error: RewriteRuleActions.createRewriteRuleError
    },

    updateRewriteRule: {
        remote(state, id, body) {
            return RewriteRuleClient.updateRewriteRule(id, body);
        },
        success: RewriteRuleActions.updateRewriteRuleSuccess,
        error: RewriteRuleActions.updateRewriteRuleError
    },

    deleteRewriteRule: {
        remote(state, id) {
            return RewriteRuleClient.deleteRewriteRule(id);
        },
        success: RewriteRuleActions.deleteRewriteRuleSuccess,
        error: RewriteRuleActions.deleteRewriteRuleError
    },

    enableRewriteRule: {
        remote(state, id) {
            return RewriteRuleClient.enableRewriteRule(id);
        },
        success: RewriteRuleActions.enableRewriteRuleSuccess,
        error: RewriteRuleActions.enableRewriteRuleError
    },

    disableRewriteRule: {
        remote(state, id) {
            return RewriteRuleClient.disableRewriteRule(id);
        },
        success: RewriteRuleActions.disableRewriteRuleSuccess,
        error: RewriteRuleActions.disableRewriteRuleError
    }
};

export default RewriteRuleDataSource;
