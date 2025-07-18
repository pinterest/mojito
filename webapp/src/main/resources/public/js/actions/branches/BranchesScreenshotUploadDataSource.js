import ImageClient from "../../sdk/ImageClient.js";
import ScreenshotClient from "../../sdk/ScreenshotClient.js";
import ScreenshotRun from "../../sdk/entity/ScreenshotRun.js";
import BranchesScreenshotUploadActions from "./BranchesScreenshotUploadActions.js";
import BranchesStore from "../../stores/branches/BranchesStore.js";
import Screenshot, { TextUnit, TmTextUnit } from "../../sdk/entity/Screenshot.js";
import uuidv4 from "uuid/v4.js";
import BranchTextUnitsStore from "../../stores/branches/BranchTextUnitsStore.js";

const BranchesScreenshotUploadDataSource = {

    performUploadScreenshotImage: {
        remote(state, generatedUuid) {
            return ImageClient.uploadImage(generatedUuid, state.imageForUpload);
        },
        success: BranchesScreenshotUploadActions.uploadScreenshotImageSuccess,
        error: BranchesScreenshotUploadActions.uploadScreenshotImageError
    },

    performUploadScreenshot: {
        remote(state) {
            const BranchesStoreState = BranchesStore.getState();

            const branchStatistic = BranchesStore.getSelectedBranchStatistic();
            const repository = branchStatistic.branch.repository;

            const screenshotRun =  new ScreenshotRun();
            screenshotRun.id = repository.manualScreenshotRun.id;

            const screenshot = new Screenshot();
            screenshotRun.screenshots.push(screenshot);

            screenshot.name = uuidv4();
            screenshot.src = state.screenshotSrc;
            screenshot.locale = repository.sourceLocale;
            screenshot.branch = branchStatistic.branch;

            let branchTextUnitStatistics;
            if (branchStatistic.isPaginated) {
                branchTextUnitStatistics = BranchTextUnitsStore.getState().branchTextUnitStatistics;
            } else {
                branchTextUnitStatistics = branchStatistic.branchTextUnitStatistics;
            }
            branchTextUnitStatistics.forEach(branchTextUnitStatistic => {
                if (BranchesStoreState.selectedBranchTextUnitIds.indexOf(branchTextUnitStatistic.id) >= 0) {

                    screenshot.textUnits.push(new TextUnit(new TmTextUnit(branchTextUnitStatistic.tmTextUnit.id)));
                }
            });

            return ScreenshotClient.createOrUpdateScreenshotRun(screenshotRun);
        },
        success: BranchesScreenshotUploadActions.uploadScreenshotSuccess,
        error: BranchesScreenshotUploadActions.uploadScreenshotError
    }
};

export default BranchesScreenshotUploadDataSource;