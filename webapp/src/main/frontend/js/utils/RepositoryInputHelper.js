// Converts "extension:checkerType,..." string to an array of objects [{assetExtension, integrityCheckerType}]
export function deserializeAssetIntegrityCheckers(assetIntegrityCheckersString) {
    if (!assetIntegrityCheckersString) return [];
    return assetIntegrityCheckersString.split(',').map(pair => {
        const [assetExtension, integrityCheckerType] = pair.trim().split(':');
        return {
            assetExtension: assetExtension && assetExtension.trim(),
            integrityCheckerType: integrityCheckerType && integrityCheckerType.trim()
        };
    });
}

// Serializes an array of objects [{assetExtension, integrityCheckerType}] to "extension:checkerType,..."
export function serializeAssetIntegrityCheckers(assetIntegrityCheckers) {
    if (!Array.isArray(assetIntegrityCheckers)) return "";
    return assetIntegrityCheckers.map(pair => {
        return `${pair.assetExtension}:${pair.integrityCheckerType}`;
    }).join(',');
}

// Validates that assetIntegrityCheckersString is in the format "extension:checkerType,..."
export function validateAssetIntegrityCheckers(assetIntegrityCheckersString) {
    if (!assetIntegrityCheckersString) return true;
    return assetIntegrityCheckersString.split(',').every(pair => {
        const [assetExtension, integrityCheckerType] = pair.trim().split(':');
        return Boolean(assetExtension) && Boolean(integrityCheckerType);
    });
}

// Converts [{parentLocale, childLocales, toBeFullyTranlsated}...] to
// [{childLocale, parentLocale, toBeFullyTranslated}...]
export function flattenRepositoryLocales(repositoryLocales) {
    const result = [];
    const childLocalesSet = new Set();

    repositoryLocales.forEach(parent => {
        if (Array.isArray(parent.childLocales)) {
            parent.childLocales.forEach(child => {
                childLocalesSet.add(child.locale);
            });
        }
    });

    repositoryLocales.forEach(parent => {
        if (!childLocalesSet.has(parent.locale)) {
            result.push({
                locale: parent.locale,
                toBeFullyTranslated: parent.toBeFullyTranslated
            });
        }

        if (Array.isArray(parent.childLocales)) {
            parent.childLocales.forEach(child => {
                result.push({
                    locale: child.locale,
                    toBeFullyTranslated: child.toBeFullyTranslated,
                    parentLocale: { locale: parent.locale }
                });
            });
        }
    });

    return result;
}

// Converts flattened repository locales back to hierarchical structure
// [{parentLocale, childLocales, toBeFullyTranslated}...] excluding the sourceLocale
export function unflattenRepositoryLocales(flattened, sourceLocale) {
    const localeMap = new Map();

    flattened.forEach(item => {
        localeMap.set(item.locale.id, {
            id: item.id,
            locale: item.locale,
            toBeFullyTranslated: item.toBeFullyTranslated,
            childLocales: [],
        });
    });

    flattened.forEach(item => {
        const parentId = item.parentLocale?.locale?.id;
        if (parentId && localeMap.has(parentId)) {
            localeMap.get(parentId).childLocales.push(localeMap.get(item.locale.id));
        }
    });

    return flattened
        .filter(item => item.parentLocale?.locale?.id === sourceLocale.id)
        .map(item => localeMap.get(item.locale.id));
}
