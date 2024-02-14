package com.box.l10n.mojito.service.repository.statistics;

public class RepositoryLocaleStatisticCalculation {

  private long translatedCount = 0;

  private long translatedWordCount = 0;

  private long translationNeededCount = 0;

  private long translationNeededWordCount = 0;

  private long reviewNeededCount = 0;

  private long reviewNeededWordCount = 0;

  private long includeInFileCount = 0;

  private long includeInFileWordCount = 0;

  private long forTranslationCount = 0;

  private long forTranslationWordCount = 0;

  public void incrementTranslatedCount() {
    translatedCount++;
  }

  public void incrementTranslatedWordCount(long wordCount) {
    translatedWordCount += wordCount;
  }

  public void incrementTranslationNeededCount() {
    translationNeededCount++;
  }

  public void incrementTranslationNeededWordCount(long wordCount) {
    translationNeededWordCount += wordCount;
  }

  public void incrementReviewNeededCount() {
    reviewNeededCount++;
  }

  public void incrementIncludeInFileCount() {
    includeInFileCount++;
  }

  public void incrementIncludeInFileWordCount(long wordCount) {
    includeInFileWordCount += wordCount;
  }

  public void incrementForTranslationCount() {
    forTranslationCount++;
  }

  public void incrementForTranslationWordCount(long wordCount) {
    forTranslationWordCount += wordCount;
  }

  public long getTranslatedCount() {
    return translatedCount;
  }

  public long getTranslatedWordCount() {
    return translatedWordCount;
  }

  public long getTranslationNeededCount() {
    return translationNeededCount;
  }

  public long getTranslationNeededWordCount() {
    return translationNeededWordCount;
  }

  public long getReviewNeededCount() {
    return reviewNeededCount;
  }

  public long getIncludeInFileCount() {
    return includeInFileCount;
  }

  public long getIncludeInFileWordCount() {
    return includeInFileWordCount;
  }

  public long getForTranslationCount() {
    return forTranslationCount;
  }

  public long getForTranslationWordCount() {
    return forTranslationWordCount;
  }

  public long getReviewNeededWordCount() {
    return reviewNeededWordCount;
  }

  public void incrementReviewNeededWordCount(long wordCount) {
    reviewNeededWordCount += wordCount;
  }
}
