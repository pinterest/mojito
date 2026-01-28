interface BranchUser {
    id: number;
    createdDate: number;
    username: string;
    commonName: string;
}

interface Locale {
    id: number;
}

interface ManualScreenshotRun {
    id: number;
    createdDate: number | null;
}

interface Repository {
    id: number;
    createdDate: number;
    name: string;
    sourceLocale: Locale;
    manualScreenshotRun: ManualScreenshotRun;
}

interface TmTextUnit {
    id: number;
    createdDate: number;
    name: string;
    content: string;
}

interface TextUnit {
    id: number;
    tmTextUnit: TmTextUnit;
}

export interface Screenshot {
    id: number;
    createdDate: number;
    src: string;
    textUnits: TextUnit[];
}

interface Branch {
    id: number;
    createdDate: number;
    repository: Repository;
    name: string;
    createdByUser: BranchUser;
    deleted: boolean;
    screenshots: Screenshot[];
}

interface BranchTextUnitStatistic {
    id: number;
    tmTextUnit: TmTextUnit;
    forTranslationCount: number;
    totalCount: number;
}

export interface BranchStatistics {
    id: number;
    createdDate: number;
    totalCount: number;
    forTranslationCount: number;
    branch: Branch;
    branchTextUnitStatistics: BranchTextUnitStatistic[];
}
