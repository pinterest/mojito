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

// Validates that assetIntegrityCheckersString is in the format "extension:checkerType,..."
export function validateAssetIntegrityCheckers(assetIntegrityCheckersString) {
    if (!assetIntegrityCheckersString) return true;
    return assetIntegrityCheckersString.split(',').every(pair => {
        const [assetExtension, integrityCheckerType] = pair.trim().split(':');
        return !!assetExtension && !!integrityCheckerType;
    });
}