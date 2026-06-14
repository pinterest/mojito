import BaseClient from "./BaseClient.js";

class RewriteRuleClient extends BaseClient {
    getRewriteRules(params) {
        return this.get(this.getUrl(), params);
    }

    createRewriteRule(body) {
        return this.post(this.getUrl(), body);
    }

    updateRewriteRule(id, body) {
        return this.put(this.getUrl() + `/${id}`, body);
    }

    deleteRewriteRule(id) {
        return this.delete(this.getUrl() + `/${id}`);
    }

    enableRewriteRule(id) {
        return this.patch(this.getUrl() + `/${id}/enable`);
    }

    disableRewriteRule(id) {
        return this.patch(this.getUrl() + `/${id}/disable`);
    }

    getEntityName() {
        return 'rewrite-rules';
    }
}

export default new RewriteRuleClient();
