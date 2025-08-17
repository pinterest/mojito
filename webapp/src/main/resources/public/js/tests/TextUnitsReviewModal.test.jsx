import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { IntlProvider } from 'react-intl';
import TextUnit from '../sdk/TextUnit';
import TextUnitsReviewModal from '../components/workbench/TextUnitsReviewModal';

// Mock messages for react-intl
const messages = {
  'textUnit.reviewModal.title': 'Review Text Units',
  'textUnit.reviewModal.rejected': 'Rejected',
  'textUnit.reviewModal.mtReview': 'MT Review Needed',
  'textUnit.reviewModal.mt': 'Machine Translated',
  'textUnit.reviewModal.needsReview': 'Needs Review',
  'textUnit.reviewModal.accepted': 'Accepted',
  'textUnit.reviewModal.overridden': 'Overridden',
  'textUnit.reviewModal.translationNeeded': 'Translation Needed',
  'textUnit.reviewModal.commentLabel': 'Comment',
  'textUnit.reviewModal.commentPlaceholder': 'Add a comment',
  'label.save': 'Save',
  'label.cancel': 'Cancel',
};

// Helper function to render component with IntlProvider
const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>
  );
};

describe('TextUnitsReviewModal', () => {
  // Mock TextUnit class instance
  const createMockTextUnit = (status, includedInLocalizedFile = true, latestSeverity = null, targetComment = '') => {
    const textUnit = new TextUnit();
    textUnit.getStatus = jest.fn().mockReturnValue(status);
    textUnit.isIncludedInLocalizedFile = jest.fn().mockReturnValue(includedInLocalizedFile);
    textUnit.getLatestSeverity = jest.fn().mockReturnValue(latestSeverity);
    textUnit.getTargetComment = jest.fn().mockReturnValue(targetComment);
    return textUnit;
  };

  const mockOnSave = jest.fn();
  const mockOnClose = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('initializes with the correct review state for rejected text units', () => {
    const rejectedTextUnit = createMockTextUnit(undefined, false, 'ERROR');
    
    renderWithIntl(
      <TextUnitsReviewModal
        isShowModal={true}
        textUnitsArray={[rejectedTextUnit]}
        onReviewModalSaveClicked={mockOnSave}
        onCloseModal={mockOnClose}
      />
    );

    // The "Rejected" button should be active
    const rejectButton = screen.getByRole('button', { name: /Rejected/i });
    expect(rejectButton).toHaveClass('active');
  });

  test('initializes with the correct review state for rejected text units with null severity', () => {
    const rejectedTextUnit = createMockTextUnit(undefined, false, null);

    renderWithIntl(
        <TextUnitsReviewModal
            isShowModal={true}
            textUnitsArray={[rejectedTextUnit]}
            onReviewModalSaveClicked={mockOnSave}
            onCloseModal={mockOnClose}
        />
    );

    // The "Rejected" button should be active
    const rejectButton = screen.getByRole('button', { name: /Rejected/i });
    expect(rejectButton).toHaveClass('active');
  });

  test('does not mark as rejected when severity is INFO', () => {
    const rejectedTextUnit = createMockTextUnit(undefined, false, 'INFO');

    renderWithIntl(
        <TextUnitsReviewModal
            isShowModal={true}
            textUnitsArray={[rejectedTextUnit]}
            onReviewModalSaveClicked={mockOnSave}
            onCloseModal={mockOnClose}
        />
    );

    // The "Rejected" button should not be active
    const rejectButton = screen.getByRole('button', { name: /Rejected/i });
    expect(rejectButton).not.toHaveClass('active');
  });
});
