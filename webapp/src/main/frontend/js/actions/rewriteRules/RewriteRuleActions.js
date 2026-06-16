import alt from "../../alt.js";

class RewriteRuleActions {
    constructor() {
        this.generateActions(
            "getAllRewriteRules",
            "getAllRewriteRulesSuccess",
            "getAllRewriteRulesError",
            "createRewriteRule",
            "createRewriteRuleSuccess",
            "createRewriteRuleError",
            "updateRewriteRule",
            "updateRewriteRuleSuccess",
            "updateRewriteRuleError",
            "deleteRewriteRule",
            "deleteRewriteRuleSuccess",
            "deleteRewriteRuleError",
            "enableRewriteRule",
            "enableRewriteRuleSuccess",
            "enableRewriteRuleError",
            "disableRewriteRule",
            "disableRewriteRuleSuccess",
            "disableRewriteRuleError",
            "setScope",
            "setPage",
            "setSelectedRepoIds",
            "setSelectedLocaleIds",
            "setRewriteFrom",
            "resetError"
        );
    }
}

export default alt.createActions(RewriteRuleActions);
