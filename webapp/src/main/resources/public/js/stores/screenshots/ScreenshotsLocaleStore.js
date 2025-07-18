import alt from "../../alt.js";
import ScreenshotsRepositoryActions from "../../actions/screenshots/ScreenshotsRepositoryActions.js";
import ScreenshotsPageActions from "../../actions/screenshots/ScreenshotsPageActions.js";
import ScreenshotsLocaleActions from "../../actions/screenshots/ScreenshotsLocaleActions.js";
import ScreenshotsRepositoryStore from "./ScreenshotsRepositoryStore.js";

class ScreenshotsLocaleStore {

    constructor() {
        this.setDefaultState();
        this.bindActions(ScreenshotsPageActions);

        this.bindListeners({
            changeSelectedBcp47Tags : ScreenshotsLocaleActions.CHANGE_SELECTED_BCP47TAGS,
            changeDropdownOpen: ScreenshotsLocaleActions.CHANGE_DROPDOWN_OPEN,
            getAllRepositoriesSuccess: ScreenshotsRepositoryActions.GET_ALL_REPOSITORIES_SUCCESS,
            changeSelectedRepositoryIds: ScreenshotsRepositoryActions.CHANGE_SELECTED_REPOSITORY_IDS
        });
    }

    setDefaultState() {
        this.bcp47Tags = [];
        this.fullyTranslatedBcp47Tags = [];
        this.selectedBcp47Tags = [];
        this.isDisabled = true;
        this.dropdownOpen = false;
    }

    resetScreenshotSearchParams() {
        this.setDefaultState();
    }

    getAllRepositoriesSuccess() {
        this.waitFor(ScreenshotsRepositoryStore);
        const selectedRepositoryIds = ScreenshotsRepositoryStore.getState().selectedRepositoryIds;
        this.bcp47Tags = this.getSortedBcp47TagsFromStore(selectedRepositoryIds);
        this.fullyTranslatedBcp47Tags = this.getSortedFullyTranslatedBcp47TagsFromStore(selectedRepositoryIds);
    }

    changeSelectedRepositoryIds(selectedRepositoryIds) {
        this.waitFor(ScreenshotsRepositoryStore);
        this.bcp47Tags = this.getSortedBcp47TagsFromStore(selectedRepositoryIds);
        this.fullyTranslatedBcp47Tags = this.getSortedFullyTranslatedBcp47TagsFromStore(selectedRepositoryIds);

        if (selectedRepositoryIds.length === 0) {
            this.selectedBcp47Tags = [];
        }
    }

    changeSelectedBcp47Tags(selectedBcp47Tags) {
        this.selectedBcp47Tags = selectedBcp47Tags.slice().sort();
    }

    changeDropdownOpen(dropdownOpen) {
        this.dropdownOpen = dropdownOpen;
    }

    /**
     * Gets sorted bcp47tags from stores.
     *
     * Sort is important to ensure later array comparison in the component will
     * work as expected.
     *
     * @returns {string[]}
     */
    getSortedBcp47TagsFromStore(selectedRepositoryIds) {
        return ScreenshotsRepositoryStore.getAllBcp47TagsForRepositoryIds(selectedRepositoryIds, false, false).sort();
    }

    /**
     * Gets sorted fully translated bcp47tags from stores.
     *
     * Sort is important to ensure later array comparison in the component will
     * work as expected.
     *
     * @returns {string[]}
     */
    getSortedFullyTranslatedBcp47TagsFromStore(selectedRepositoryIds) {
        return ScreenshotsRepositoryStore.getAllBcp47TagsForRepositoryIds(selectedRepositoryIds, true, false).sort();
    }
}

export default alt.createStore(ScreenshotsLocaleStore, 'ScreenshotsLocaleStore');
