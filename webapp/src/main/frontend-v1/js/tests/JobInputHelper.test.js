import {
  parseLocaleMappingString,
  serializeLocaleMappingArray,
  serializeOptionsArray,
  parseOptionsArray
} from '../utils/JobInputHelper.js';

describe('parseLocaleMappingString', () => {
  it('returns empty array for empty or non-string input', () => {
    expect(parseLocaleMappingString('')).toEqual([]);
    expect(parseLocaleMappingString(null)).toEqual([]);
    expect(parseLocaleMappingString(undefined)).toEqual([]);
    expect(parseLocaleMappingString(123)).toEqual([]);
  });
  it('parses single mapping', () => {
    expect(parseLocaleMappingString('en:en-US')).toEqual([
      { key: 'en', value: 'en-US' }
    ]);
  });
  it('parses multiple mappings with spaces', () => {
    expect(parseLocaleMappingString('en:en-US, fr: fr-FR')).toEqual([
      { key: 'en', value: 'en-US' },
      { key: 'fr', value: 'fr-FR' }
    ]);
  });
  it('filters out empty pairs', () => {
    expect(parseLocaleMappingString('en:en-US, : , :fr-FR')).toEqual([
      { key: 'en', value: 'en-US' },
      { key: '', value: 'fr-FR' }
    ]);
  });
});

describe('serializeLocaleMappingArray', () => {
  it('returns empty string for non-array', () => {
    expect(serializeLocaleMappingArray(null)).toBe('');
    expect(serializeLocaleMappingArray(undefined)).toBe('');
  });
  it('serializes single mapping', () => {
    expect(serializeLocaleMappingArray([
      { key: 'en', value: 'en-US' }
    ])).toBe('en:en-US');
  });
  it('serializes multiple mappings', () => {
    expect(serializeLocaleMappingArray([
      { key: 'en', value: 'en-US' },
      { key: 'fr', value: 'fr-FR' }
    ])).toBe('en:en-US, fr:fr-FR');
  });
  it('filters out pairs with missing key or value', () => {
    expect(serializeLocaleMappingArray([
      { key: 'en', value: 'en-US' },
      { key: '', value: 'fr-FR' },
      { key: 'fr', value: '' }
    ])).toBe('en:en-US');
  });
});

describe('serializeOptionsArray', () => {
  it('returns empty array for non-array', () => {
    expect(serializeOptionsArray(null)).toEqual([]);
    expect(serializeOptionsArray(undefined)).toEqual([]);
  });
  it('serializes single option', () => {
    expect(serializeOptionsArray([
      { key: 'foo', value: 'bar' }
    ])).toEqual(['foo=bar']);
  });
  it('serializes multiple options', () => {
    expect(serializeOptionsArray([
      { key: 'foo', value: 'bar' },
      { key: 'baz', value: 'qux' }
    ])).toEqual(['foo=bar', 'baz=qux']);
  });
  it('filters out pairs with missing key or value', () => {
    expect(serializeOptionsArray([
      { key: 'foo', value: 'bar' },
      { key: '', value: 'qux' },
      { key: 'baz', value: '' }
    ])).toEqual(['foo=bar']);
  });
});

describe('parseOptionsArray', () => {
  it('returns empty array for non-array', () => {
    expect(parseOptionsArray(null)).toEqual([]);
    expect(parseOptionsArray(undefined)).toEqual([]);
  });
  it('parses single option', () => {
    expect(parseOptionsArray(['foo=bar'])).toEqual([
      { key: 'foo', value: 'bar' }
    ]);
  });
  it('parses multiple options', () => {
    expect(parseOptionsArray(['foo=bar', 'baz=qux'])).toEqual([
      { key: 'foo', value: 'bar' },
      { key: 'baz', value: 'qux' }
    ]);
  });
  it('handles missing key or value', () => {
    expect(parseOptionsArray(['foo=', '=bar', '='])).toEqual([
      { key: 'foo', value: '' },
      { key: '', value: 'bar' }
    ]);
  });
});
