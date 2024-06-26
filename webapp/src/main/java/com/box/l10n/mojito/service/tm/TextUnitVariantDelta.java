package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.service.delta.DeltaType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Interface that captures information about a text unit variant together with information about its
 * source text unit and delta type.
 *
 * @author garion
 */
public interface TextUnitVariantDelta {
  String getTextUnitName();

  String getBcp47Tag();

  String getContent();

  @Enumerated(EnumType.STRING)
  DeltaType getDeltaType();
}
