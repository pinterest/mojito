import type { TFunction } from "i18next";
import { describe, expect, it } from "vitest";
import {
    buildTextUnitCountDictionary,
    createLocaleToTextUnitStatusMap,
    extractAvailableStatuses,
    extractLocales,
    type LocaleStatusMap,
    transformToChartData,
} from "./localeStatusChartUtils";
import { getTextUnitScreenshotMap } from "./textUnitStatusVisualization";
import type {
    BranchTextUnitStatusDto,
    TextUnitStatusDto,
} from "@/types/branchTextUnitStatus";
import type { TextUnitStatus } from "@/types/textUnitStatus";
import type { BranchStatistics } from "@/types/branchStatistics";

function generateTextUnit(status: TextUnitStatus): TextUnitStatusDto {
    return {
        textUnitId: 1,
        textUnitName: "Sample Text Unit",
        variantId: null,
        currentVariantId: null,
        modifiedDate: null,
        status: status,
        content: Math.random().toString(36).substring(7),
        comment: null,
    };
}

const DEFAULT_BRANCH_DATA: BranchTextUnitStatusDto = {
    branchId: 1,
    repositoryName: "repo",
    branchName: "main",
    localeTextUnitStatus: {},
};

const identityFn = (x: string) => x;

describe("localeStatusChartUtils", () => {
    describe("extractLocales", () => {
        it("should return empty array when no locales exist", () => {
            const mockData = {
                ...DEFAULT_BRANCH_DATA,
                localeTextUnitStatus: {},
            };

            const result = extractLocales(mockData);

            expect(result).toEqual([]);
        });

        it("should handle single locale", () => {
            const mockData = {
                ...DEFAULT_BRANCH_DATA,
                localeTextUnitStatus: {
                    "en-US": [generateTextUnit("APPROVED")],
                },
            };

            const result = extractLocales(mockData);

            expect(result).toEqual(["en-US"]);
        });

        it("should extract locale keys from branch text unit status", () => {
            const mockData = {
                ...DEFAULT_BRANCH_DATA,
                localeTextUnitStatus: {
                    "en-US": [generateTextUnit("APPROVED")],
                    "fr-FR": [generateTextUnit("TRANSLATION_NEEDED")],
                    "ja-JP": [generateTextUnit("REVIEW_NEEDED")],
                },
            };

            const result = extractLocales(mockData);

            expect(result).toEqual(["en-US", "fr-FR", "ja-JP"]);
        });
    });

    describe("extractStatuses", () => {
        it("should return empty array when no text units exist", () => {
            const mockData = {
                ...DEFAULT_BRANCH_DATA,
                localeTextUnitStatus: {
                    "en-US": [],
                },
            };

            const result = extractAvailableStatuses(mockData);

            expect(result).toEqual([]);
        });

        it("should extract and order unique statuses from all text units", () => {
            const mockData = {
                ...DEFAULT_BRANCH_DATA,
                localeTextUnitStatus: {
                    "en-US": [
                        generateTextUnit("APPROVED"),
                        generateTextUnit("TRANSLATION_NEEDED"),
                        generateTextUnit("REVIEW_NEEDED"),
                    ],
                    "fr-FR": [
                        generateTextUnit("APPROVED"),
                        generateTextUnit("MT_TRANSLATED"),
                    ],
                },
            };

            const result = extractAvailableStatuses(mockData);
            expect(result).toContain("APPROVED");
            expect(result).toContain("MT_TRANSLATED");
            expect(result).toContain("REVIEW_NEEDED");
            expect(result).toContain("TRANSLATION_NEEDED");
            expect(new Set(result).size).toBe(result.length);
        });

        it("should default null status to TRANSLATION_NEEDED", () => {
            const mockData = {
                ...DEFAULT_BRANCH_DATA,
                localeTextUnitStatus: {
                    "en-US": [
                        generateTextUnit("APPROVED"),
                        generateTextUnit(null as unknown as TextUnitStatus),
                    ],
                },
            };

            const result = extractAvailableStatuses(mockData);

            expect(result).toContain("TRANSLATION_NEEDED");
            expect(result).toContain("APPROVED");
        });

        it("should handle undefined status as TRANSLATION_NEEDED", () => {
            const mockData = {
                ...DEFAULT_BRANCH_DATA,
                localeTextUnitStatus: {
                    "en-US": [
                        generateTextUnit(
                            undefined as unknown as TextUnitStatus,
                        ),
                        generateTextUnit("REVIEW_NEEDED"),
                    ],
                },
            };

            const result = extractAvailableStatuses(mockData);

            expect(result).toContain("TRANSLATION_NEEDED");
            expect(result).toContain("REVIEW_NEEDED");
        });
    });

    describe("createLocaleStatusMap", () => {
        it("should handle empty text units array", () => {
            const mockData = {
                ...DEFAULT_BRANCH_DATA,
                localeTextUnitStatus: {
                    "en-US": [],
                    "fr-FR": [generateTextUnit("APPROVED")],
                },
            };

            const result = createLocaleToTextUnitStatusMap(mockData);

            expect(result).toEqual({
                "en-US": {},
                "fr-FR": {
                    APPROVED: 1,
                },
            });
        });

        it("should create correct locale to status count mapping", () => {
            const mockData = {
                ...DEFAULT_BRANCH_DATA,
                localeTextUnitStatus: {
                    "en-US": [
                        generateTextUnit("APPROVED"),
                        generateTextUnit("APPROVED"),
                        generateTextUnit("REVIEW_NEEDED"),
                    ],
                    "fr-FR": [
                        generateTextUnit("TRANSLATION_NEEDED"),
                        generateTextUnit("MT_TRANSLATED"),
                    ],
                },
            };

            const result = createLocaleToTextUnitStatusMap(mockData);

            expect(result).toEqual({
                "en-US": {
                    APPROVED: 2,
                    REVIEW_NEEDED: 1,
                },
                "fr-FR": {
                    TRANSLATION_NEEDED: 1,
                    MT_TRANSLATED: 1,
                },
            });
        });

        it("should handle null status as TRANSLATION_NEEDED", () => {
            const mockData = {
                ...DEFAULT_BRANCH_DATA,
                localeTextUnitStatus: {
                    "en-US": [
                        generateTextUnit(null as unknown as TextUnitStatus),
                        generateTextUnit(null as unknown as TextUnitStatus),
                        generateTextUnit("APPROVED"),
                    ],
                },
            };

            const result = createLocaleToTextUnitStatusMap(mockData);

            expect(result).toEqual({
                "en-US": {
                    TRANSLATION_NEEDED: 2,
                    APPROVED: 1,
                },
            });
        });
    });

    describe("transformToChartData", () => {
        it("should handle empty locales array", () => {
            const locales: string[] = [];
            const statuses: TextUnitStatus[] = ["APPROVED"];
            const localeStatusMap: LocaleStatusMap = {};

            const result = transformToChartData(
                locales,
                statuses,
                localeStatusMap,
                identityFn as TFunction<string, string>,
            );

            expect(result.labels).toEqual([]);
            expect(result.datasets).toHaveLength(1);
            expect(result.datasets[0].data).toEqual([]);
        });

        it("should handle empty statuses array", () => {
            const locales = ["en-US"];
            const statuses: TextUnitStatus[] = [];
            const localeStatusMap: LocaleStatusMap = {
                "en-US": { APPROVED: 1 },
            };

            const result = transformToChartData(
                locales,
                statuses,
                localeStatusMap,
                identityFn as TFunction<string, string>,
            );

            expect(result.labels).toEqual(["en-US"]);
            expect(result.datasets).toEqual([]);
        });

        it("should handle missing status counts with zero", () => {
            const locales = ["en-US", "fr-FR"];
            const statuses: TextUnitStatus[] = ["APPROVED", "REVIEW_NEEDED"];
            const localeStatusMap: LocaleStatusMap = {
                "en-US": {
                    APPROVED: 2,
                    // REVIEW_NEEDED missing
                },
                "fr-FR": {
                    REVIEW_NEEDED: 1,
                    // APPROVED missing
                },
            };

            const result = transformToChartData(
                locales,
                statuses,
                localeStatusMap,
                identityFn as TFunction<string, string>,
            );

            // Should fill missing values with 0
            const approvedDataset = result.datasets.find(
                (ds) => ds.label === "approved",
            );
            expect(approvedDataset?.data).toEqual([2, 0]);

            const reviewDataset = result.datasets.find(
                (ds) => ds.label === "reviewNeeded",
            );
            expect(reviewDataset?.data).toEqual([0, 1]);
        });

        it("should transform data into correct chart format", () => {
            const locales = ["en-US", "fr-FR"];
            const statuses: TextUnitStatus[] = [
                "APPROVED",
                "TRANSLATION_NEEDED",
            ];
            const localeStatusMap: LocaleStatusMap = {
                "en-US": {
                    APPROVED: 3,
                    TRANSLATION_NEEDED: 1,
                },
                "fr-FR": {
                    APPROVED: 2,
                    TRANSLATION_NEEDED: 4,
                },
            };

            const result = transformToChartData(
                locales,
                statuses,
                localeStatusMap,
                identityFn as TFunction<string, string>,
            );

            expect(result.labels).toEqual(["en-US", "fr-FR"]);
            expect(result.datasets).toHaveLength(2);

            const approvedDataset = result.datasets.find(
                (ds) => ds.label === "approved",
            );
            expect(approvedDataset).toBeDefined();
            expect(approvedDataset?.data).toEqual([3, 2]);

            const translationDataset = result.datasets.find(
                (ds) => ds.label === "translationNeeded",
            );
            expect(translationDataset).toBeDefined();
            expect(translationDataset?.data).toEqual([1, 4]);
        });

        it("should handle locale not present in status map", () => {
            const locales = ["en-US", "de-DE"];
            const statuses: TextUnitStatus[] = ["APPROVED"];
            const localeStatusMap: LocaleStatusMap = {
                "en-US": {
                    APPROVED: 3,
                },
                // de-DE not in map
            };

            const result = transformToChartData(
                locales,
                statuses,
                localeStatusMap,
                identityFn as TFunction<string, string>,
            );

            const approvedDataset = result.datasets.find(
                (ds) => ds.label === "approved",
            );
            expect(approvedDataset?.data).toEqual([3, 0]);
        });

        it("should ignore unspecified locales in status map", () => {
            const locales = ["de-DE"];
            const statuses: TextUnitStatus[] = ["APPROVED"];
            const localeStatusMap: LocaleStatusMap = {
                "en-US": {
                    APPROVED: 3,
                },
            };

            const result = transformToChartData(
                locales,
                statuses,
                localeStatusMap,
                identityFn as TFunction<string, string>,
            );

            const approvedDataset = result.datasets.find(
                (ds) => ds.label === "approved",
            );
            expect(approvedDataset?.data).toEqual([0]);
        });
    });

    describe("buildTextUnitCountDictionary", () => {
        it("should return empty dictionary when no locales exist", () => {
            const mockData = {
                ...DEFAULT_BRANCH_DATA,
                localeTextUnitStatus: {},
            };

            const result = buildTextUnitCountDictionary(mockData);

            expect(result).toEqual({});
        });

        it("should handle empty text units arrays", () => {
            const mockData = {
                ...DEFAULT_BRANCH_DATA,
                localeTextUnitStatus: {
                    "en-US": [],
                    "fr-FR": [],
                },
            };

            const result = buildTextUnitCountDictionary(mockData);

            expect(result).toEqual({});
        });

        it("should count single text unit correctly", () => {
            const mockData = {
                ...DEFAULT_BRANCH_DATA,
                localeTextUnitStatus: {
                    "en-US": [generateTextUnit("APPROVED")],
                },
            };

            const result = buildTextUnitCountDictionary(mockData);

            expect(result).toEqual({
                APPROVED: 1,
            });
        });

        it("should count multiple text units of same status", () => {
            const mockData = {
                ...DEFAULT_BRANCH_DATA,
                localeTextUnitStatus: {
                    "en-US": [
                        generateTextUnit("APPROVED"),
                        generateTextUnit("APPROVED"),
                        generateTextUnit("APPROVED"),
                    ],
                },
            };

            const result = buildTextUnitCountDictionary(mockData);

            expect(result).toEqual({
                APPROVED: 3,
            });
        });

        it("should count multiple text units of different statuses", () => {
            const mockData = {
                ...DEFAULT_BRANCH_DATA,
                localeTextUnitStatus: {
                    "en-US": [
                        generateTextUnit("APPROVED"),
                        generateTextUnit("REVIEW_NEEDED"),
                        generateTextUnit("APPROVED"),
                        generateTextUnit("MT_TRANSLATED"),
                    ],
                },
            };

            const result = buildTextUnitCountDictionary(mockData);

            expect(result).toEqual({
                APPROVED: 2,
                REVIEW_NEEDED: 1,
                MT_TRANSLATED: 1,
            });
        });

        it("should aggregate counts across multiple locales", () => {
            const mockData = {
                ...DEFAULT_BRANCH_DATA,
                localeTextUnitStatus: {
                    "en-US": [
                        generateTextUnit("APPROVED"),
                        generateTextUnit("REVIEW_NEEDED"),
                    ],
                    "fr-FR": [
                        generateTextUnit("APPROVED"),
                        generateTextUnit("MT_TRANSLATED"),
                    ],
                    "ja-JP": [
                        generateTextUnit("REVIEW_NEEDED"),
                        generateTextUnit("TRANSLATION_NEEDED"),
                    ],
                },
            };

            const result = buildTextUnitCountDictionary(mockData);

            expect(result).toEqual({
                APPROVED: 2,
                REVIEW_NEEDED: 2,
                MT_TRANSLATED: 1,
                TRANSLATION_NEEDED: 1,
            });
        });

        it("should handle convert null/undefined to TRANSLATION_NEEDED status count", () => {
            const mockData = {
                ...DEFAULT_BRANCH_DATA,
                localeTextUnitStatus: {
                    "en-US": [
                        generateTextUnit("APPROVED"),
                        generateTextUnit(null as unknown as TextUnitStatus),
                        generateTextUnit("APPROVED"),
                        generateTextUnit(
                            undefined as unknown as TextUnitStatus,
                        ),
                    ],
                },
            };

            const result = buildTextUnitCountDictionary(mockData);

            expect(result).toEqual({
                APPROVED: 2,
                TRANSLATION_NEEDED: 2,
            });
        });
    });

    describe("getTextUnitScreenshots", () => {
        const createMockBranchStatistics = (
            screenshots: BranchStatistics["branch"]["screenshots"] = [],
        ): BranchStatistics =>
            ({
                branch: {
                    screenshots: screenshots,
                },
            }) as BranchStatistics;

        const createMockScreenshot = (
            id: number,
            src: string,
            createdDate: string,
            textUnits: Array<{ id: number }>,
        ): BranchStatistics["branch"]["screenshots"][0] => ({
            id,
            src,
            createdDate: new Date(createdDate).getTime(),
            textUnits: textUnits.map((tu, i) => {
                return {
                    id: i,
                    tmTextUnit: {
                        id: tu.id,
                        createdDate: 0,
                        name: "",
                        content: "",
                    },
                };
            }),
        });

        it("should return empty map when branchStats is undefined", () => {
            const result = getTextUnitScreenshotMap(
                undefined as unknown as BranchStatistics,
            );

            expect(result).toBeInstanceOf(Map);
            expect(result.size).toBe(0);
        });

        it("should return empty map when branchStats is null", () => {
            const result = getTextUnitScreenshotMap(
                null as unknown as BranchStatistics,
            );

            expect(result).toBeInstanceOf(Map);
            expect(result.size).toBe(0);
        });

        it("should return empty map when branch is undefined", () => {
            const branchStats = {} as BranchStatistics;
            const result = getTextUnitScreenshotMap(branchStats);

            expect(result).toBeInstanceOf(Map);
            expect(result.size).toBe(0);
        });

        it("should return empty map when screenshots array is undefined", () => {
            const branchStats = createMockBranchStatistics();
            branchStats.branch!.screenshots =
                undefined as unknown as BranchStatistics["branch"]["screenshots"];
            const result = getTextUnitScreenshotMap(branchStats);

            expect(result).toBeInstanceOf(Map);
            expect(result.size).toBe(0);
        });

        it("should return empty map when screenshots array is empty", () => {
            const branchStats = createMockBranchStatistics([]);
            const result = getTextUnitScreenshotMap(branchStats);

            expect(result).toBeInstanceOf(Map);
            expect(result.size).toBe(0);
        });

        it("should handle single screenshot with single text unit", () => {
            const screenshot = createMockScreenshot(
                1,
                "screenshot1.png",
                "2024-01-01T10:00:00Z",
                [{ id: 101 }],
            );
            const branchStats = createMockBranchStatistics([screenshot]);

            const result = getTextUnitScreenshotMap(branchStats);

            expect(result.size).toBe(1);
            expect(result.get(101)?.src).toBe("screenshot1.png");
        });

        it("should handle single screenshot with multiple text units", () => {
            const screenshot = createMockScreenshot(
                1,
                "screenshot1.png",
                "2024-01-01T10:00:00Z",
                [{ id: 101 }, { id: 102 }, { id: 103 }],
            );
            const branchStats = createMockBranchStatistics([screenshot]);

            const result = getTextUnitScreenshotMap(branchStats);

            expect(result.size).toBe(3);
            expect(result.get(101)?.src).toBe("screenshot1.png");
            expect(result.get(102)?.src).toBe("screenshot1.png");
            expect(result.get(103)?.src).toBe("screenshot1.png");
        });

        it("should handle multiple screenshots with different text units", () => {
            const screenshots = [
                createMockScreenshot(
                    1,
                    "screenshot1.png",
                    "2024-01-01T10:00:00Z",
                    [{ id: 101 }],
                ),
                createMockScreenshot(
                    2,
                    "screenshot2.png",
                    "2024-01-01T11:00:00Z",
                    [{ id: 102 }],
                ),
            ];
            const branchStats = createMockBranchStatistics(screenshots);

            const result = getTextUnitScreenshotMap(branchStats);

            expect(result.size).toBe(2);
            expect(result.get(101)?.src).toBe("screenshot1.png");
            expect(result.get(102)?.src).toBe("screenshot2.png");
        });

        it("should keep most recent screenshot when same text unit appears in multiple screenshots", () => {
            const screenshots = [
                createMockScreenshot(
                    1,
                    "old-screenshot.png",
                    "2024-01-01T10:00:00Z",
                    [{ id: 101 }, { id: 102 }],
                ),
                createMockScreenshot(
                    2,
                    "new-screenshot.png",
                    "2024-01-01T12:00:00Z",
                    [{ id: 101 }],
                ),
            ];
            const branchStats = createMockBranchStatistics(screenshots);

            const result = getTextUnitScreenshotMap(branchStats);

            expect(result.size).toBe(2);
            expect(result.get(101)?.src).toBe("new-screenshot.png");
            expect(result.get(102)?.src).toBe("old-screenshot.png");
        });

        it("should keep most recent screenshot when processing in reverse chronological order", () => {
            const oldDate = "2024-01-01T10:00:00Z";
            const newDate = new Date(
                new Date(oldDate).getTime() + 2 * 60 * 60 * 1000, // +2 hours
            ).toISOString();
            const screenshots = [
                createMockScreenshot(2, "newer-screenshot.png", newDate, [
                    { id: 101 },
                ]),
                createMockScreenshot(1, "older-screenshot.png", oldDate, [
                    { id: 101 },
                ]),
            ];
            const branchStats = createMockBranchStatistics(screenshots);

            const result = getTextUnitScreenshotMap(branchStats);

            expect(result.size).toBe(1);
            expect(result.get(101)?.src).toBe("newer-screenshot.png");
        });

        it("should handle complex scenario with multiple text units and overlapping screenshots", () => {
            const screenshots = [
                createMockScreenshot(
                    1,
                    "screenshot1.png",
                    "2024-01-01T10:00:00Z",
                    [{ id: 101 }, { id: 102 }],
                ),
                createMockScreenshot(
                    2,
                    "screenshot2.png",
                    "2024-01-01T11:00:00Z",
                    [
                        { id: 102 }, // Overrides 102
                        { id: 103 },
                    ],
                ),
                createMockScreenshot(
                    3,
                    "screenshot3.png",
                    "2024-01-01T09:00:00Z",
                    [
                        { id: 101 }, // Should not override 101 because it's older
                        { id: 104 },
                    ],
                ),
            ];
            const branchStats = createMockBranchStatistics(screenshots);

            const result = getTextUnitScreenshotMap(branchStats);

            expect(result.size).toBe(4);
            expect(result.get(101)?.src).toBe("screenshot1.png");
            expect(result.get(102)?.src).toBe("screenshot2.png");
            expect(result.get(103)?.src).toBe("screenshot2.png");
            expect(result.get(104)?.src).toBe("screenshot3.png");
        });

        it("should ignore screenshots with empty textUnits arrays", () => {
            const screenshots = [
                createMockScreenshot(
                    1,
                    "screenshot1.png",
                    "2024-01-01T10:00:00Z",
                    [],
                ),
                createMockScreenshot(
                    2,
                    "screenshot2.png",
                    "2024-01-01T11:00:00Z",
                    [{ id: 101 }],
                ),
            ];
            const branchStats = createMockBranchStatistics(screenshots);

            const result = getTextUnitScreenshotMap(branchStats);

            expect(result.size).toBe(1);
            expect(result.get(101)?.src).toBe("screenshot2.png");
        });

        it("should handle date comparison with different date formats", () => {
            const screenshots = [
                createMockScreenshot(
                    1,
                    "first.png",
                    "2024-01-01T10:00:00.000Z",
                    [{ id: 101 }],
                ),
                createMockScreenshot(2, "second.png", "2024-01-01T10:00:01Z", [
                    { id: 101 },
                ]),
            ];
            const branchStats = createMockBranchStatistics(screenshots);

            const result = getTextUnitScreenshotMap(branchStats);

            expect(result.size).toBe(1);
            expect(result.get(101)?.src).toBe("second.png");
        });

        it("should handle same timestamps by keeping the last processed", () => {
            const timestamp = "2024-01-01T10:00:00Z";
            const screenshots = [
                createMockScreenshot(1, "first.png", timestamp, [{ id: 101 }]),
                createMockScreenshot(2, "second.png", timestamp, [{ id: 101 }]),
            ];
            const branchStats = createMockBranchStatistics(screenshots);

            const result = getTextUnitScreenshotMap(branchStats);

            expect(result.size).toBe(1);
            expect(result.get(101)?.src).toBe("second.png");
        });
    });
});
