/**
 * Deserializes a string of asset integrity checkers into an array of objects.
 * Example input: "xml:PO,properties:JavaProperties"
 * Output: [ { assetExtension: "xml", integrityCheckerType: "PO" }, ... ]
 */
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

/**
 * Validates the asset integrity checkers string.
 * Returns true if the string is valid, false otherwise.
 * Valid format: "ext:type,ext:type,..."
 */
export function validateAssetIntegrityCheckers(assetIntegrityCheckersString) {
    if (!assetIntegrityCheckersString) return true;
    return assetIntegrityCheckersString.split(',').every(pair => {
        const [assetExtension, integrityCheckerType] = pair.trim().split(':');
        return !!assetExtension && !!integrityCheckerType;
    });
}