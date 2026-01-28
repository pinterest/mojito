package com.box.l10n.mojito.service.branch;

import com.box.l10n.mojito.entity.TMTextUnitVariant;
import java.time.ZonedDateTime;

public record TextUnitStatusDTO(
    Long textUnitId,
    Long variantId,
    Long currentVariantId,
    ZonedDateTime modifiedDate,
    TMTextUnitVariant.Status status,
    String textUnitName,
    String content,
    String comment) {}
