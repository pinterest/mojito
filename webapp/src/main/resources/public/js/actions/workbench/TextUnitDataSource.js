import Error from "../../utils/Error.js";
import TextUnitError from "../../utils/TextUnitError.js";
import TextUnitClient from "../../sdk/TextUnitClient.js";
import WorkbenchActions from "./WorkbenchActions.js";
import GitBlameActions from "./GitBlameActions.js";
import TranslationHistoryActions from "./TranslationHistoryActions.js";

const TextUnitDataSource = {
    performSaveTextUnit: {
        remote(searchResultsStoreState, textUnit) {
            return TextUnitClient.saveTextUnit(textUnit)
                .catch(() => {
                    throw new TextUnitError(Error.IDS.TEXTUNIT_SAVE_FAILED, textUnit);
                });
        },
        success: WorkbenchActions.saveTextUnitSuccess,
        error: WorkbenchActions.saveTextUnitError
    },
    performCheckAndSaveTextUnit: {
        remote(searchResultsStoreState, textUnit) {
            return TextUnitClient.checkTextUnitIntegrity(textUnit)
                .then(checkResult => {
                    if (checkResult && !checkResult.checkResult) {
                        throw new TextUnitError(Error.IDS.TEXTUNIT_CHECK_FAILED, textUnit);
                    }
                }).then(() => {
                    return TextUnitClient.saveTextUnit(textUnit)
                        .catch(() => {
                            throw new TextUnitError(Error.IDS.TEXTUNIT_SAVE_FAILED, textUnit);
                        });
                });
        },
        success: WorkbenchActions.checkAndSaveTextUnitSuccess,
        error: WorkbenchActions.checkAndSaveTextUnitError
    },
    deleteTextUnit: {
        remote(searchResultsStoreState, textUnit) {
            return TextUnitClient.deleteCurrentTranslation(textUnit)
                .catch(() => {
                    throw new TextUnitError(Error.IDS.TEXTUNIT_DELETE_FAILED, textUnit);
                });
        },
        success: WorkbenchActions.deleteTextUnitsSuccess,
        error: WorkbenchActions.deleteTextUnitsError
    },

    saveVirtualAssetTextUnit: {
        remote(searchResultsStoreState, textUnit) {
            return TextUnitClient.saveVirtualAssetTextUnit(textUnit)
                .catch(() => {
                    throw new TextUnitError(Error.IDS.VIRTUAL_ASSET_TEXTUNIT_SAVE_FAILED, textUnit);
                });
        },
        success: WorkbenchActions.saveVirtualAssetTextUnitSuccess,
        error: WorkbenchActions.saveVirtualAssetTextUnitError
    },

    getGitBlameInfo: {
        remote(gitBlameStoreState, textUnit) {
            return TextUnitClient.getGitBlameInfo(textUnit);
        },
        success: GitBlameActions.getGitBlameInfoSuccess,
        error: GitBlameActions.getGitBlameInfoError
    },

    getTranslationHistory: {
        remote(translationHistoryStoreState, textUnit) {
            return TextUnitClient.getTranslationHistory(textUnit);
        },
        success: TranslationHistoryActions.getTranslationHistorySuccess,
        error: TranslationHistoryActions.getTranslationHistoryError
    }
};

export default TextUnitDataSource;
