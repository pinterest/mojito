import BaseClient from "./BaseClient.js";
import TextUnit from "./TextUnit.js";
import TextUnitIntegrityCheckRequest from "./textunit/TextUnitIntegrityCheckRequest.js";
import TextUnitIntegrityCheckResult from "./textunit/TextUnitIntegrityCheckResult.js";
import PollableTaskClient from "./PollableTaskClient.js";


class TextUnitClient extends BaseClient {

    /**
     * Gets the text units that matches the searcher parameters.
     *
     * Uses an HTTP POST to support larger number of parameters
     *
     * @param {TextUnitSearcherParameters} textUnitSearcherParameters
     *
     * @returns {Promise.<TextUnit[]|err>} a promise that retuns an array of text units
     */
    getTextUnits(textUnitSearcherParameters) {
        const promise = this.post(this.getUrl() + '/search', textUnitSearcherParameters.getParams());

        return promise.then(function (result) {
            return TextUnit.toTextUnits(result);
        });
    }

    /**
     * Gets the counts for text units which match the parameters.
     *
     * @param {TextUnitSearcherParameters} textUnitSearcherParameters
     *
     * @returns {Promise.<TextUnit[]|err>} a promise that retuns an array of text units
     */
    getTextUnitCount(textUnitSearcherParameters) {
        return this.get(this.getUrl() + '/count', textUnitSearcherParameters.getParams());
    }

    /**
     * Deletes the current text unit.
     *
     * @param {TextUnit} textUnit
     * @returns {Promise}
     */
    deleteCurrentTranslation(textUnit) {
        return this.delete(this.getUrl(textUnit.getTmTextUnitCurrentVariantId())).then(function () {
            textUnit.setTarget(null);
            textUnit.setTranslated(false);
            return textUnit;
        });
    }

    /**
     * Saves a TextUnit.
     *
     * @param {TextUnit} textUnit
     * @returns {Promise<TextUnit, err>} a promise that returns the updated or created text unit
     */
    saveTextUnit(textUnit) {
        return this.post(this.getUrl(), textUnit.data).then(function (jsonTextUnit) {
            return TextUnit.toTextUnit(jsonTextUnit);
        });
    }

    /**
     * Saves a TextUnit.
     *
     * @param {TextUnit} textUnit
     * @returns {Promise<TextUnitIntegrityCheckResult, err>} a promise that returns the updated or created text unit
     */
    checkTextUnitIntegrity(textUnit) {
        const request = new TextUnitIntegrityCheckRequest();
        request.content = textUnit.getTarget();
        request.tmTextUnitId = textUnit.getTmTextUnitId();

        return this.post(this.getUrl() + '/check', request).then(function (jsonTextUnit) {
            return new TextUnitIntegrityCheckResult(jsonTextUnit);
        });
    }

    /**
     * Saves a VirtualAssetTextUnit build of TextUnit information.
     *
     * @param {TextUnit} textUnit
     * @returns
     */
    saveVirtualAssetTextUnit(textUnit) {
        return this.post(this.getAssetTextUnitsUrl(textUnit.getAssetId()), [{
            name: textUnit.getName(),
            content: textUnit.getSource(),
            comment: textUnit.getComment(),
            pluralForm: textUnit.getPluralForm(),
            pluralFormOther: textUnit.getPluralFormOther(),
            doNotTranslate: textUnit.getDoNotTranslate(),
        }]).then(function (pollableTask) {
            return PollableTaskClient.waitForPollableTaskToFinish(pollableTask.id).then(function (pollableTask) {
                if (pollableTask.errorMessage) {
                    throw new Error(pollableTask.errorMessage);
                }

                return textUnit;
            });
        });
    }

    getAssetTextUnitsUrl(assetId) {
        return this.baseUrl + 'virtualAssets/' + assetId + '/textUnits';
    }

    /**
     * Gets the GitBlameWithUsage that matches the given textUnit.
     * @param textUnit
     * @returns {Promise}
     */
    getGitBlameInfo(textUnit) {
        return this.get(this.getUrl() + "/gitBlameWithUsages", { "tmTextUnitId": textUnit.getTmTextUnitId() });
    }

    getTranslationHistory(textUnit) {
        return this.get(this.getUrl(textUnit.getTmTextUnitId()) + "/history", {
            "bcp47Tag": textUnit.getTargetLocale()
        });
    }

    getEntityName() {
        return 'textunits';
    }
}
;

export default new TextUnitClient();



