package com.box.l10n.mojito.rest.openai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.entity.AIPrompt;
import com.box.l10n.mojito.rest.WSTestBase;
import com.box.l10n.mojito.service.openai.OpenAIService;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

public class OpenAIWSTest extends WSTestBase {

  @Autowired private OpenAIWS openAIWS;

  @Mock private OpenAIService openAIService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    openAIWS.openAIService = openAIService;
  }

  @Test
  public void testExecuteAIChecks() throws Exception {
    OpenAICheckRequest request = new OpenAICheckRequest();
    OpenAICheckResponse expectedResponse = new OpenAICheckResponse();
    when(openAIService.executeAIChecks(request)).thenReturn(expectedResponse);

    OpenAICheckResponse actualResponse = openAIWS.executeAIChecks(request);
    assertEquals(expectedResponse, actualResponse);
    verify(openAIService).executeAIChecks(request);
  }

  @Test
  public void testExecuteAIChecksFailure() throws Exception {
    OpenAICheckRequest request = new OpenAICheckRequest();
    when(openAIService.executeAIChecks(request))
        .thenThrow(new OpenAIException("Failure in processing"));

    OpenAICheckResponse response = openAIWS.executeAIChecks(request);
    assertTrue(response.isError());
    assertEquals("Failure in processing", response.getErrorMessage());
  }

  @Test
  public void testCreatePrompt() {
    OpenAIPromptCreateRequest request = new OpenAIPromptCreateRequest();
    openAIWS.createPrompt(request);
    verify(openAIService, times(1)).createPrompt(request);
  }

  @Test
  public void testDeletePrompt() {
    Long promptId = 1L;
    openAIWS.deletePrompt(promptId);
    verify(openAIService, times(1)).deletePrompt(promptId);
  }

  @Test
  public void testGetPrompt() {
    Long promptId = 1L;
    AIPrompt aiPrompt = new AIPrompt();
    when(openAIService.getPrompt(promptId)).thenReturn(aiPrompt);

    OpenAIPrompt prompt = openAIWS.getPrompt(promptId);
    assertNotNull(prompt);
    verify(openAIService, times(1)).getPrompt(promptId);
  }

  @Test
  public void testGetAllActivePrompts() {
    AIPrompt aiPrompt1 = new AIPrompt();
    AIPrompt aiPrompt2 = new AIPrompt();
    when(openAIService.getAllActivePrompts()).thenReturn(Arrays.asList(aiPrompt1, aiPrompt2));

    List<OpenAIPrompt> prompts = openAIWS.getAllActivePrompts();
    assertEquals(2, prompts.size());
    verify(openAIService, times(1)).getAllActivePrompts();
  }

  @Test
  public void testGetAllActivePromptsForRepository() {
    String repositoryName = "repo1";
    AIPrompt aiPrompt1 = new AIPrompt();
    AIPrompt aiPrompt2 = new AIPrompt();

    when(openAIService.getAllActivePromptsForRepository(repositoryName))
        .thenReturn(Arrays.asList(aiPrompt1, aiPrompt2));

    List<OpenAIPrompt> prompts = openAIWS.getAllActivePromptsForRepository(repositoryName);
    assertEquals(2, prompts.size());
    verify(openAIService, times(1)).getAllActivePromptsForRepository(repositoryName);
  }
}
