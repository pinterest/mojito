import React from 'react';
import { render, screen } from '@testing-library/react';
import { IntlProvider } from 'react-intl';
import TextUnitSDK from '../sdk/TextUnit';

// Create a base store mock factory to reuse
const createStoreMock = () => ({
  getState: jest.fn().mockReturnValue({}),
  listen: jest.fn(),
  unlisten: jest.fn(),
  addChangeListener: jest.fn(),
  removeChangeListener: jest.fn()
});

const TextUnitStoreMock = createStoreMock();
TextUnitStoreMock.getError = jest.fn().mockReturnValue(null);

const SearchResultsStoreMock = createStoreMock();
SearchResultsStoreMock.getState = jest.fn().mockReturnValue({});

global.TextUnitStore = TextUnitStoreMock;
global.SearchResultsStore = SearchResultsStoreMock;

import TextUnit from '../components/workbench/TextUnit';

// Mock messages for react-intl
const messages = {
  'textUnit.tag.asset': 'Asset',
  'textUnit.tag.repo': 'Repository', // Add this missing message ID
  'workbench.gitBlameModal.info': 'Git Blame Info',
  'workbench.translationHistoryModal.info': 'Translation History',
  'textUnit.reviewModal.rejected': 'Rejected',
  'textUnit.reviewModal.mtReview': 'MT Review Needed',
  'textUnit.reviewModal.mt': 'Machine Translated',
  'textUnit.reviewModal.needsReview': 'Needs Review',
  'textUnit.reviewModal.accepted': 'Accepted',
  'textUnit.reviewModal.overridden': 'Overridden',
  'textUnit.reviewModal.translationNeeded': 'Translation Needed',
};

// Helper function to render component with IntlProvider
const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>
  );
};

describe('TextUnit', () => {
  // Mock TextUnit instance
  const createMockTextUnit = () => {
    const textUnit = new TextUnitSDK();
    textUnit.data = {
      tmTextUnitId: 1,
      name: 'test_string_id',
      content: 'Source string content',
      comment: 'This is a comment',
      targetLocale: 'fr-FR',
      repositoryName: 'test-repository',
      assetPath: 'path/to/asset.json',
      translated: true,
      target: 'Contenu de chaîne source',
      included: true,
      status: TextUnitSDK.STATUS.APPROVED
    };
    
    textUnit.getTmTextUnitId = jest.fn().mockReturnValue(textUnit.data.tmTextUnitId);
    textUnit.getName = jest.fn().mockReturnValue(textUnit.data.name);
    textUnit.getSource = jest.fn().mockReturnValue(textUnit.data.content);
    textUnit.getComment = jest.fn().mockReturnValue(textUnit.data.comment);
    textUnit.getTargetLocale = jest.fn().mockReturnValue(textUnit.data.targetLocale);
    textUnit.getRepositoryName = jest.fn().mockReturnValue(textUnit.data.repositoryName);
    textUnit.getAssetPath = jest.fn().mockReturnValue(textUnit.data.assetPath);
    textUnit.isTranslated = jest.fn().mockReturnValue(textUnit.data.translated);
    textUnit.getTarget = jest.fn().mockReturnValue(textUnit.data.target);
    textUnit.isIncludedInLocalizedFile = jest.fn().mockReturnValue(textUnit.data.included);
    textUnit.getStatus = jest.fn().mockReturnValue(textUnit.data.status);
    textUnit.getPluralForm = jest.fn().mockReturnValue(null);
    textUnit.isUsed = jest.fn().mockReturnValue(true);
    textUnit.getDoNotTranslate = jest.fn().mockReturnValue(false);
    textUnit.getLatestSeverity = jest.fn().mockReturnValue(null);
    textUnit.setIncludedInLocalizedFile = jest.fn();
    textUnit.setStatus = jest.fn();
    textUnit.setTarget = jest.fn();
    textUnit.setTranslated = jest.fn();
    textUnit.setTargetComment = jest.fn();
    
    return textUnit;
  };

  const defaultProps = {
    textUnit: createMockTextUnit(),
    translation: 'Contenu de chaîne source',
    onEditModeSetToTrue: jest.fn(),
    onEditModeSetToFalse: jest.fn(),
    textUnitIndex: 1,
    isActive: false,
    isSelected: false,
    viewMode: { viewMode: 'FULL' }
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('renders rejected indicator when text unit is not included in localized file', () => {
    const textUnit = createMockTextUnit();
    textUnit.isIncludedInLocalizedFile = jest.fn().mockReturnValue(false);
    
    renderWithIntl(
      <TextUnit 
        {...defaultProps}
        textUnit={textUnit}
      />
    );
        
    const commentTextArea = screen.queryByTitle('Rejected');
    expect(commentTextArea).not.toBeNull();
  });

  test('renders rejected indicator with error severity when text unit is not included in localized file', () => {
    const textUnit = createMockTextUnit();
    textUnit.isIncludedInLocalizedFile = jest.fn().mockReturnValue(false);
    textUnit.getLatestSeverity = jest.fn().mockReturnValue("ERROR");

    renderWithIntl(
      <TextUnit 
        {...defaultProps}
        textUnit={textUnit}
      />
    );
        
    const commentTextArea = screen.queryByTitle('Rejected');
    expect(commentTextArea).not.toBeNull();
  });

  test('does not render rejected indicator when severity is INFO and not included in localized file', () => {
    const textUnit = createMockTextUnit();
    textUnit.setStatus = jest.fn().mockReturnValue(TextUnitSDK.STATUS.TRANSLATION_NEEDED);
    textUnit.isIncludedInLocalizedFile = jest.fn().mockReturnValue(false);
    textUnit.getLatestSeverity = jest.fn().mockReturnValue("INFO");

    renderWithIntl(
      <TextUnit 
        {...defaultProps}
        textUnit={textUnit}
      />
    );

    const commentTextArea = screen.queryByTitle('Rejected');
    expect(commentTextArea).toBeNull();
  });
});
