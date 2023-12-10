package com.box.l10n.mojito.service.cache;

import static org.junit.Assert.*;

import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ApplicationCacheUpdaterServiceTest extends ServiceTestBase {

  @Autowired ApplicationCacheUpdaterService applicationCacheUpdaterService;

  @Test
  public void bla() {
    System.out.println("coucou");
    final Object singleResult =
        applicationCacheUpdaterService
            .entityManager
            .createNativeQuery("SELECT TOP 1 CURRENT_TIMESTAMP FROM INFORMATION_SCHEMA.TABLES")
            .getSingleResult();
    System.out.println(singleResult);
  }
}
