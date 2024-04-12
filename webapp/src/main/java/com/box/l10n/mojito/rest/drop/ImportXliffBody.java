package com.box.l10n.mojito.rest.drop;

import com.box.l10n.mojito.entity.TMTextUnitVariant;

/**
 * @author jaurambault
 */
public class ImportXliffBody {

  /** repository in which to import the XILFF */
  Long repositoryId;

  /** if the content is from a {@link TranslationKit} */
  boolean translationKit = true;

  /** Specific status to use when importing translation */
  TMTextUnitVariant.Status importStatus;

  /**
   * The content of the localized XLIFF to be imported (for the request), and the imported xliff
   * with meta information (in the response)
   */
  String xliffContent;

  public Long getRepositoryId() {
    return repositoryId;
  }

  public void setRepositoryId(Long repositoryId) {
    this.repositoryId = repositoryId;
  }

  public boolean isTranslationKit() {
    return translationKit;
  }

  public void setTranslationKit(boolean translationKit) {
    this.translationKit = translationKit;
  }

  public TMTextUnitVariant.Status getImportStatus() {
    return importStatus;
  }

  public void setImportStatus(TMTextUnitVariant.Status importStatus) {
    this.importStatus = importStatus;
  }

  public String getXliffContent() {
    return xliffContent;
  }

  public void setXliffContent(String xliffContent) {
    this.xliffContent = xliffContent;
  }
}
