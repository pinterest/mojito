package com.box.l10n.mojito.sarif;

import static org.junit.jupiter.api.Assertions.*;

import com.box.l10n.mojito.sarif.builder.SarifBuilder;
import com.box.l10n.mojito.sarif.model.Location;
import com.box.l10n.mojito.sarif.model.Result;
import com.box.l10n.mojito.sarif.model.ResultLevel;
import com.box.l10n.mojito.sarif.model.Run;
import com.box.l10n.mojito.sarif.model.Sarif;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SarifBuilderTest {

  @Test
  void testBuildSimpleRun() {
    Sarif sarif = new SarifBuilder().addRun("MyTool", "https://tool.example").build();

    assertNotNull(sarif);
    assertEquals(1, sarif.runs.size());
    Run run = sarif.runs.get(0);
    assertNotNull(run.getTool());
    assertEquals("MyTool", run.getTool().getDriver().getName());
    assertEquals("https://tool.example", run.getTool().getDriver().getInformationUri());
    assertTrue(run.getResults().isEmpty());
  }

  @Test
  void testAddResultWithLocation_StartLineOnly() {
    Sarif sarif =
        new SarifBuilder()
            .addRun("MyTool", "https://tool.example")
            .addResultWithLocation(
                "RULE_A", ResultLevel.ERROR, "Critical error detected", "src/Main.java", 101)
            .build();

    Run run = sarif.runs.get(0);
    assertEquals(1, run.getResults().size());
    Result result = run.getResults().get(0);

    assertEquals("RULE_A", result.getRuleId());
    assertEquals(ResultLevel.ERROR, result.getLevel());
    assertEquals("Critical error detected", result.getMessage().text);

    assertEquals(1, result.getLocations().size());
    Location location = result.getLocations().get(0);
    assertEquals("src/Main.java", location.physicalLocation.artifactLocation.uri);
    assertEquals(101, location.physicalLocation.region.startLine);
    assertFalse(location.physicalLocation.region.endLine.isPresent());
  }

  @Test
  void testAddResultWithLocation_WithEndLine() {
    Sarif sarif =
        new SarifBuilder()
            .addRun("MyTool", "https://tool.example")
            .addResultWithLocation(
                "RULE_B",
                ResultLevel.WARNING,
                "Check multi-line block",
                "src/Main.java",
                120,
                Optional.of(125))
            .build();

    Run run = sarif.runs.get(0);
    assertEquals(1, run.getResults().size());
    Result result = run.getResults().get(0);

    assertEquals("RULE_B", result.getRuleId());
    assertEquals(ResultLevel.WARNING, result.getLevel());
    assertEquals("Check multi-line block", result.getMessage().text);

    assertEquals(1, result.getLocations().size());
    Location location = result.getLocations().get(0);
    assertEquals("src/Main.java", location.physicalLocation.artifactLocation.uri);
    assertEquals(120, location.physicalLocation.region.startLine);
    assertTrue(location.physicalLocation.region.endLine.isPresent());
    assertEquals(125, location.physicalLocation.region.endLine.get().intValue());
  }

  @Test
  void testMultipleResultsAndRuns() {
    SarifBuilder builder =
        new SarifBuilder()
            .addRun("Tool1", "uri1")
            .addResultWithLocation("RULE_1", ResultLevel.NOTE, "Info", "file1", 1)
            .addResultWithLocation("RULE_2", ResultLevel.ERROR, "Err", "file1", 2, Optional.of(5));
    // Build and verify
    Sarif sarif = builder.build();
    assertEquals(1, sarif.runs.size());
    Run run = sarif.runs.get(0);
    assertEquals(2, run.getResults().size());
    assertEquals("RULE_1", run.getResults().get(0).getRuleId());
    assertEquals("RULE_2", run.getResults().get(1).getRuleId());

    // Second run (make sure builder can be reused for future extension)
    SarifBuilder builder2 =
        new SarifBuilder()
            .addRun("Tool2", "uri2")
            .addResultWithLocation("RULE_X", ResultLevel.WARNING, "Warn", "x.java", 10);
    Sarif sarif2 = builder2.build();
    assertEquals(1, sarif2.runs.size());
    assertEquals("Tool2", sarif2.runs.get(0).getTool().getDriver().getName());
  }
}
