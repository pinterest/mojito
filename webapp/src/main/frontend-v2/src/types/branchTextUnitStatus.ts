import type { TextUnitStatus } from "./textUnitStatus";

interface TextUnitStatusDto {
    textUnitId: number;
    variantId: number | null;
    currentVariantId: number | null;
    modifiedDate: string | null;
    status: TextUnitStatus | null;
    content: string;
    comment: string | null;
}

export interface BranchTextUnitStatusDto {
    branchId: number;
    repositoryName: string;
    branchName: string;
    localeTextUnitStatus: {
        [locale: string]: TextUnitStatusDto[];
    };
}
