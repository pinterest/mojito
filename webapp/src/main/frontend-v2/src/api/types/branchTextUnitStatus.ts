import type { TextUnitStatus } from "@/types/textUnitStatus";

export interface TextUnitStatusDto {
  textUnitId: number;
  variantId: number | null;
  currentVariantId: number | null;
  modifiedDate: string | null;
  status: TextUnitStatus | null;
  textUnitName: string;
  content: string;
  comment: string | null;
}

export interface BranchTextUnitStatusDto {
  branchId: number;
  repositoryName: string;
  branchName: string;
  srcLocaleBcpTag: string;
  localeTextUnitStatus: {
    [locale: string]: TextUnitStatusDto[];
  };
}
