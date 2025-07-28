import {
  deserializeAssetIntegrityCheckers,
  serializeAssetIntegrityCheckers,
  validateAssetIntegrityCheckers,
  flattenRepositoryLocales,
  unflattenRepositoryLocales
} from '../utils/RepositoryInputHelper.js';

describe('deserializeAssetIntegrityCheckers', () => {
  it('returns empty array for empty string', () => {
    expect(deserializeAssetIntegrityCheckers('')).toEqual([]);
  });

  it('parses single pair', () => {
    expect(deserializeAssetIntegrityCheckers('js:checker1')).toEqual([
      { assetExtension: 'js', integrityCheckerType: 'checker1' }
    ]);
  });

  it('parses multiple pairs', () => {
    expect(deserializeAssetIntegrityCheckers('js:checker1,css:checker2')).toEqual([
      { assetExtension: 'js', integrityCheckerType: 'checker1' },
      { assetExtension: 'css', integrityCheckerType: 'checker2' }
    ]);
  });

  it('trims whitespace', () => {
    expect(deserializeAssetIntegrityCheckers(' js : checker1 , css : checker2 ')).toEqual([
      { assetExtension: 'js', integrityCheckerType: 'checker1' },
      { assetExtension: 'css', integrityCheckerType: 'checker2' }
    ]);
  });
});

describe('serializeAssetIntegrityCheckers', () => {
  it('returns empty string for non-array', () => {
    expect(serializeAssetIntegrityCheckers(null)).toBe('');
    expect(serializeAssetIntegrityCheckers(undefined)).toBe('');
  });

  it('serializes single pair', () => {
    expect(serializeAssetIntegrityCheckers([
      { assetExtension: 'js', integrityCheckerType: 'checker1' }
    ])).toBe('js:checker1');
  });

  it('serializes multiple pairs', () => {
    expect(serializeAssetIntegrityCheckers([
      { assetExtension: 'js', integrityCheckerType: 'checker1' },
      { assetExtension: 'css', integrityCheckerType: 'checker2' }
    ])).toBe('js:checker1,css:checker2');
  });
});

describe('validateAssetIntegrityCheckers', () => {
  it('returns true for empty string', () => {
    expect(validateAssetIntegrityCheckers('')).toBe(true);
  });

  it('returns true for valid pairs', () => {
    expect(validateAssetIntegrityCheckers('js:checker1')).toBe(true);
    expect(validateAssetIntegrityCheckers('js:checker1,css:checker2')).toBe(true);
  });

  it('returns false for missing assetExtension or checkerType', () => {
    expect(validateAssetIntegrityCheckers('js:')).toBe(false);
    expect(validateAssetIntegrityCheckers(':checker1')).toBe(false);
    expect(validateAssetIntegrityCheckers('js:checker1,:checker2')).toBe(false);
    expect(validateAssetIntegrityCheckers('js:checker1,css:')).toBe(false);
  });
});

describe('flattenRepositoryLocales', () => {
  it('flattens hierarchical locales', () => {
    const input = [
      {
        locale: 'en',
        toBeFullyTranslated: true,
        childLocales: [
          { locale: 'fr', toBeFullyTranslated: false },
          { locale: 'de', toBeFullyTranslated: false }
        ]
      },
      {
        locale: 'es',
        toBeFullyTranslated: true,
        childLocales: []
      }
    ];
    const result = flattenRepositoryLocales(input);
    expect(result).toEqual([
      { locale: 'en', toBeFullyTranslated: true },
      { locale: 'fr', toBeFullyTranslated: false, parentLocale: { locale: 'en' } },
      { locale: 'de', toBeFullyTranslated: false, parentLocale: { locale: 'en' } },
      { locale: 'es', toBeFullyTranslated: true }
    ]);
  });

  it('handles empty input', () => {
    expect(flattenRepositoryLocales([])).toEqual([]);
  });
});

describe('unflattenRepositoryLocales', () => {
  it('reconstructs hierarchy from flattened', () => {
    const sourceLocale = { id: 1, locale: 'en' };
    const flattened = [
      { id: 2, locale: { id: 2, locale: 'fr' }, toBeFullyTranslated: true, parentLocale: { locale: { id: 1, locale: 'en' } } },
      { id: 3, locale: { id: 3, locale: 'de' }, toBeFullyTranslated: false, parentLocale: { locale: { id: 1, locale: 'en' } } }
    ];
    const result = unflattenRepositoryLocales(flattened, sourceLocale);
    expect(result.length).toBe(2);
    expect(result[0].locale.locale).toBe('fr');
    expect(result[1].locale.locale).toBe('de');
    expect(result[0].childLocales).toEqual([]);
  });

  it('filters out child locale if parent locale is invalid', () => {
    const sourceLocale = { id: 1, locale: 'en' };
    const flattened = [
      { id: 2, locale: { id: 2, locale: 'fr' }, toBeFullyTranslated: true, parentLocale: { locale: { id: 1, locale: 'en' } } },
      { id: 3, locale: { id: 3, locale: 'de' }, toBeFullyTranslated: false, parentLocale: { locale: { id: 42, locale: 'invalid' } } }
    ];
    const result = unflattenRepositoryLocales(flattened, sourceLocale);
    expect(result.length).toBe(1);
    expect(result[0].locale.locale).toBe('fr');
    expect(result[0].childLocales).toEqual([]);
  });
  
  it('handles empty flattened', () => {
    expect(unflattenRepositoryLocales([], { id: 1, locale: 'en' })).toEqual([]);
  });
});
