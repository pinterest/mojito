import type { User } from "./user";

interface GoogleAnalytics {
    enabled: boolean;
    hashedUserId: boolean;
    trackingId: string | null;
}

interface RepositoryLinkDetail {
    location: string | null;
    commit: string | null;
    thirdParty: {
        url: string;
        label: string | null;
    };
    pullRequest: string | null;
    textUnitNameToTextUnitNameInSource: string | null;
    customMd5: string | null;
}

export interface AppConfig {
    contextPath: string;
    csrfToken: string;
    googleAnalytics: GoogleAnalytics;
    ict: boolean;
    locale: string;
    user: User;
    userMenuLogoutHidden: boolean;
    link: {
        [repoName: string]: RepositoryLinkDetail;
    };
    repositoryStatistics: {
        computeOutOfSla: boolean;
    };
    security: {
        unauthRedirectTo: string | null;
        oAuth2: Record<string, unknown>;
    };
}