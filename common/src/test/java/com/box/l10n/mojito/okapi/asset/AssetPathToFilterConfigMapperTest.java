package com.box.l10n.mojito.okapi.asset;

import static org.junit.Assert.assertEquals;

import com.box.l10n.mojito.okapi.filters.*;
import org.junit.Test;

/**
 * @author aloison
 */
public class AssetPathToFilterConfigMapperTest {

  @Test
  public void testGetFilterConfigIdFromTypeWithCSV() throws Exception {

    AssetPathToFilterConfigMapper assetPathToFilterConfigMapper =
        new AssetPathToFilterConfigMapper();
    String filterConfigId =
        assetPathToFilterConfigMapper.getFilterConfigIdFromPath("/path/to/file.csv");

    assertEquals(CSVFilter.FILTER_CONFIG_ID, filterConfigId);
  }

  @Test
  public void testGetFilterConfigIdFromTypeWithXLIFF1() throws Exception {

    AssetPathToFilterConfigMapper assetPathToFilterConfigMapper =
        new AssetPathToFilterConfigMapper();
    String filterConfigId =
        assetPathToFilterConfigMapper.getFilterConfigIdFromPath("/path/to/file.xlf");

    assertEquals(XliffFilter.FILTER_CONFIG_ID, filterConfigId);
  }

  @Test
  public void testGetFilterConfigIdFromTypeWithXLIFF2() throws Exception {

    AssetPathToFilterConfigMapper assetPathToFilterConfigMapper =
        new AssetPathToFilterConfigMapper();
    String filterConfigId =
        assetPathToFilterConfigMapper.getFilterConfigIdFromPath("/path/to/file.xliff");

    assertEquals(XliffFilter.FILTER_CONFIG_ID, filterConfigId);
  }

  @Test(expected = UnsupportedAssetFilterTypeException.class)
  public void testGetFilterConfigIdFromTypeWithUnsupportedType() throws Exception {

    AssetPathToFilterConfigMapper assetPathToFilterConfigMapper =
        new AssetPathToFilterConfigMapper();
    assetPathToFilterConfigMapper.getFilterConfigIdFromPath(
        "/path/to/file/with/unsupported/type.ext");
  }

  @Test
  public void testXLIFFFilterConfigIdConstMatchesOkapiConfigId() throws Exception {

    String okapiConfigId = new XliffFilter().getName();
    assertEquals(XliffFilter.FILTER_CONFIG_ID, okapiConfigId);
  }

  @Test
  public void testGetFilterConfigIdFromTypeWithAndroidStrings() throws Exception {

    AssetPathToFilterConfigMapper assetPathToFilterConfigMapper =
        new AssetPathToFilterConfigMapper();
    String filterConfigId =
        assetPathToFilterConfigMapper.getFilterConfigIdFromPath("/path/to/strings.xml");

    assertEquals(AndroidFilter.FILTER_CONFIG_ID, filterConfigId);
  }

  @Test
  public void testGetFilterConfigIdFromTypeWithMacStrings() throws Exception {

    AssetPathToFilterConfigMapper assetPathToFilterConfigMapper =
        new AssetPathToFilterConfigMapper();
    String filterConfigId =
        assetPathToFilterConfigMapper.getFilterConfigIdFromPath("/path/to/Localizable.strings");

    assertEquals(AssetPathToFilterConfigMapper.MACSTRINGS_FILTER_CONFIG_ID, filterConfigId);
  }

  @Test
  public void testGetFilterConfigIdFromTypeWithMacStringsdict() throws Exception {

    AssetPathToFilterConfigMapper assetPathToFilterConfigMapper =
        new AssetPathToFilterConfigMapper();
    String filterConfigId =
        assetPathToFilterConfigMapper.getFilterConfigIdFromPath("/path/to/Localizable.stringsdict");

    assertEquals(MacStringsdictFilter.FILTER_CONFIG_ID, filterConfigId);
  }

  @Test
  public void testGetFilterConfigIdFromTypeWithResx() throws Exception {

    AssetPathToFilterConfigMapper assetPathToFilterConfigMapper =
        new AssetPathToFilterConfigMapper();
    String filterConfigId =
        assetPathToFilterConfigMapper.getFilterConfigIdFromPath("/path/to/File.resx");

    assertEquals(AssetPathToFilterConfigMapper.RESX_FILTER_CONFIG_ID, filterConfigId);
  }

  @Test
  public void testGetFilterConfigIdFromTypeWithXtb() throws Exception {

    AssetPathToFilterConfigMapper assetPathToFilterConfigMapper =
        new AssetPathToFilterConfigMapper();
    String filterConfigId =
        assetPathToFilterConfigMapper.getFilterConfigIdFromPath("/path/to/File.xtb");

    assertEquals(AssetPathToFilterConfigMapper.XTB_FILTER_CONFIG_ID, filterConfigId);
  }

  @Test
  public void testGetFilterConfigIdFromTypeWithProperties() throws Exception {

    AssetPathToFilterConfigMapper assetPathToFilterConfigMapper =
        new AssetPathToFilterConfigMapper();
    String filterConfigId =
        assetPathToFilterConfigMapper.getFilterConfigIdFromPath("/path/to/File.properties");

    assertEquals(AssetPathToFilterConfigMapper.PROPERTIES_FILTER_CONFIG_ID, filterConfigId);
  }

  @Test
  public void testGetFilterConfigIdFromTypeWithPo() throws Exception {

    AssetPathToFilterConfigMapper assetPathToFilterConfigMapper =
        new AssetPathToFilterConfigMapper();
    String filterConfigId =
        assetPathToFilterConfigMapper.getFilterConfigIdFromPath("/path/to/File.pot");
    assertEquals(POFilter.FILTER_CONFIG_ID, filterConfigId);
  }

  @Test
  public void testGetFilterConfigIdFromTypeWithTS() throws Exception {

    AssetPathToFilterConfigMapper assetPathToFilterConfigMapper =
        new AssetPathToFilterConfigMapper();
    String filterConfigId =
        assetPathToFilterConfigMapper.getFilterConfigIdFromPath("/path/to/File.ts");
    assertEquals(AssetPathToFilterConfigMapper.JS_FILTER_CONFIG_ID, filterConfigId);
  }

  @Test
  public void testGetFilterConfigIdFromTypeWithJS() throws Exception {

    AssetPathToFilterConfigMapper assetPathToFilterConfigMapper =
        new AssetPathToFilterConfigMapper();
    String filterConfigId =
        assetPathToFilterConfigMapper.getFilterConfigIdFromPath("/path/to/File.js");
    assertEquals(AssetPathToFilterConfigMapper.JS_FILTER_CONFIG_ID, filterConfigId);
  }

  @Test
  public void testGetFilterConfigIdFromTypeWithJSON() throws Exception {
    AssetPathToFilterConfigMapper assetPathToFilterConfigMapper =
        new AssetPathToFilterConfigMapper();
    String filterConfigId =
        assetPathToFilterConfigMapper.getFilterConfigIdFromPath("/path/to/strings.json");
    assertEquals(JSONFilter.FILTER_CONFIG_ID, filterConfigId);
  }

  @Test
  public void testGetFilterConfigIdFromTypeWithYAML() throws Exception {

    AssetPathToFilterConfigMapper assetPathToFilterConfigMapper =
        new AssetPathToFilterConfigMapper();
    String filterConfigId =
        assetPathToFilterConfigMapper.getFilterConfigIdFromPath("/path/to/File.yaml");
    assertEquals(YamlFilter.FILTER_CONFIG_ID, filterConfigId);
  }
}
