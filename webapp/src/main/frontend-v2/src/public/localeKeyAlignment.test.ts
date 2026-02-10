import { describe, expect, it } from "vitest";
import fs from "fs";
import path from "path";

describe("Translation Locale Key Consistency", () => {
  const translationsDir = path.join(__dirname, "./locales");

  const getTranslationFiles = (): { [locale: string]: string[] } => {
    if (!fs.existsSync(translationsDir)) {
      throw new Error(`Translations directory not found: ${translationsDir}`);
    }
    const localeFiles: { [locale: string]: string[] } = {};
    const localeDirs = fs.readdirSync(translationsDir).filter((item) => {
      const fullPath = path.join(translationsDir, item);
      return fs.statSync(fullPath).isDirectory();
    });

    for (const localeDir of localeDirs) {
      const localePath = path.join(translationsDir, localeDir);
      const jsonFiles = fs
        .readdirSync(localePath)
        .filter((file) => file.endsWith(".json"))
        .map((file) => path.join(localePath, file));
      localeFiles[localeDir] = jsonFiles;
    }

    return localeFiles;
  };

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const getAllKeys = (obj: any, prefix = ""): string[] => {
    let keys: string[] = [];
    for (const key in obj) {
      const fullKey = prefix ? `${prefix}.${key}` : key;
      if (typeof obj[key] === "object" && obj[key] !== null) {
        keys = keys.concat(getAllKeys(obj[key], fullKey));
      } else {
        keys.push(fullKey);
      }
    }
    return keys.sort();
  };

  it("should have consistent translation keys across all languages", () => {
    const translationFiles = getTranslationFiles();
    expect(Object.keys(translationFiles).length).toBeGreaterThan(0);

    const allTranslations: {
      [language: string]: { [filename: string]: string[] };
    } = {};

    for (const locale in translationFiles) {
      const files = translationFiles[locale];
      allTranslations[locale] = {};

      for (const file of files) {
        const filename = path.basename(file, ".json");
        const content = JSON.parse(fs.readFileSync(file, "utf8"));
        allTranslations[locale][filename] = getAllKeys(content);
      }
    }

    const languages = Object.keys(allTranslations);
    const referenceLanguage = languages[0];
    const referenceFiles = allTranslations[referenceLanguage];

    for (const filename in referenceFiles) {
      const referenceKeys = referenceFiles[filename];
      const referenceKeySet = new Set(referenceKeys);

      for (let i = 1; i < languages.length; i++) {
        const currentLanguage = languages[i];

        expect(
          allTranslations[currentLanguage][filename],
          `Language '${currentLanguage}' is missing file: ${filename}.json`,
        ).toBeDefined();

        const currentKeys = allTranslations[currentLanguage][filename];
        const currentKeySet = new Set(currentKeys);

        const missingKeys = referenceKeys.filter(
          (key) => !currentKeySet.has(key),
        );
        const extraKeys = currentKeys.filter(
          (key) => !referenceKeySet.has(key),
        );

        expect(
          missingKeys,
          `File '${filename}.json' in language '${currentLanguage}' is missing keys: ${missingKeys.join(", ")}`,
        ).toHaveLength(0);

        expect(
          extraKeys,
          `File '${filename}.json' in language '${currentLanguage}' has extra keys: ${extraKeys.join(", ")}`,
        ).toHaveLength(0);
      }
    }

    for (let i = 1; i < languages.length; i++) {
      const currentLanguage = languages[i];
      const currentFiles = Object.keys(allTranslations[currentLanguage]);
      const referenceFileSet = new Set(Object.keys(referenceFiles));

      const extraFiles = currentFiles.filter(
        (filename) => !referenceFileSet.has(filename),
      );

      expect(
        extraFiles,
        `Language '${currentLanguage}' has extra files: ${extraFiles.map((f) => f + ".json").join(", ")}`,
      ).toHaveLength(0);
    }
  });

  it("should not have empty translation values", () => {
    const translationFiles = getTranslationFiles();
    const checkForEmptyValues = (obj: Record<string, unknown>): string[] => {
      const emptyKeys: string[] = [];
      for (const [key, value] of Object.entries(obj)) {
        if (!value || String(value).trim() === "") {
          emptyKeys.push(key);
        }
      }
      return emptyKeys;
    };

    for (const [locale, files] of Object.entries(translationFiles)) {
      for (const file of files) {
        const filename = path.basename(file, ".json");
        const content = JSON.parse(fs.readFileSync(file, "utf8"));

        const emptyKeys = checkForEmptyValues(content);
        expect(
          emptyKeys,
          `Language '${locale}' in file '${filename}.json' has empty values for keys: ${emptyKeys.join(", ")}`,
        ).toHaveLength(0);
      }
    }
  });
});
