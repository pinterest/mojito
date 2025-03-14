import UserStatics from "./UserStatics";

class AuthorityService {
    static userHasPermission(componentName) {
        const authorityLevel = this.componentName2AuthorityLevel(componentName);
        return authorityLevel.includes(APP_CONFIG.user.role);
    }

    static componentName2AuthorityLevel(componentName){
        const admin = UserStatics.authorityAdmin();
        const pm = UserStatics.authorityPm();
        const translator = UserStatics.authorityTranslator();

        const level=[];

        switch (componentName) {
            case "view-jobs":
            case "edit-screenshots":
            case "project-requests":
            case "user-management":
                level.push(admin, pm);
                break;
            case "edit-translations":
                level.push(translator, admin, pm);
                break;
        }

        return level;
    }

    static canViewUserManagement() {
        return this.userHasPermission("user-management");
    }

    static canEditProjectRequests() {
        return this.userHasPermission("project-requests");
    }

    static canEditTranslations() {
        return this.userHasPermission("edit-translations");
    }

    static canEditScreenshots() {
        return this.userHasPermission("edit-screenshots");
    }

    static canViewJobs() {
        return this.userHasPermission("view-jobs");
    }
}

export default AuthorityService;
