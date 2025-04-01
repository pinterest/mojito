package com.box.l10n.mojito.service.evolve;

import static com.box.l10n.mojito.evolve.TranslationStatusType.IN_TRANSLATION;
import static com.box.l10n.mojito.evolve.TranslationStatusType.READY_FOR_TRANSLATION;
import static com.box.l10n.mojito.evolve.TranslationStatusType.TRANSLATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.BranchStatistic;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.evolve.CourseDTO;
import com.box.l10n.mojito.evolve.CoursesGetRequest;
import com.box.l10n.mojito.evolve.EvolveClient;
import com.box.l10n.mojito.evolve.EvolveConfigurationProperties;
import com.box.l10n.mojito.evolve.TranslationStatusType;
import com.box.l10n.mojito.okapi.asset.UnsupportedAssetFilterTypeException;
import com.box.l10n.mojito.rest.asset.SourceAsset;
import com.box.l10n.mojito.service.NormalizationUtils;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionByBranchRepository;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.assetcontent.AssetContentRepository;
import com.box.l10n.mojito.service.branch.BranchRepository;
import com.box.l10n.mojito.service.branch.BranchService;
import com.box.l10n.mojito.service.branch.BranchStatisticRepository;
import com.box.l10n.mojito.service.branch.BranchStatisticService;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.repository.RepositoryLocaleCreationException;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.box.l10n.mojito.xliff.XliffUtils;
import com.google.common.collect.Sets;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

public class EvolveServiceTest extends ServiceTestBase {

  @Autowired RepositoryService repositoryService;

  @Autowired AssetService assetService;

  @Autowired PollableTaskService pollableTaskService;

  @Autowired XliffUtils xliffUtils;

  @Autowired LocaleService localeService;

  @Autowired BranchRepository branchRepository;

  @Autowired BranchStatisticRepository branchStatisticRepository;

  @Autowired BranchStatisticService branchStatisticService;

  @Autowired AssetContentRepository assetContentRepository;

  @Autowired TMService tmService;

  @Autowired TextUnitSearcher textUnitSearcher;

  @Autowired BranchService branchService;

  @Autowired AssetExtractionByBranchRepository assetExtractionByBranchRepository;

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Mock EvolveClient evolveClient;

  @Captor ArgumentCaptor<Integer> integerCaptor;

  @Captor ArgumentCaptor<String> stringCaptor;

  @Captor ArgumentCaptor<Set<String>> additionalLocalesCaptor;

  @Captor ArgumentCaptor<TranslationStatusType> translationStatusTypeCaptor;

  EvolveService evolveService;

  EvolveConfigurationProperties evolveConfigurationProperties;

  @Before
  public void before() {
    evolveConfigurationProperties = new EvolveConfigurationProperties();
    evolveConfigurationProperties.setRepositoryName(testIdWatcher.getEntityName("test"));
  }

  private void initData(ZonedDateTime updatedOn) {
    CourseDTO courseDTO1 = new CourseDTO();
    courseDTO1.setId(1);
    courseDTO1.setTranslationStatus(READY_FOR_TRANSLATION);
    courseDTO1.setUpdatedOn(updatedOn);

    when(this.evolveClient.getCourses(any(CoursesGetRequest.class)))
        .thenReturn(Stream.of(courseDTO1));
    when(this.evolveClient.startCourseTranslation(anyInt(), anyString(), anySet()))
        .thenReturn(
            """
        <?xml version="1.0" encoding="UTF-8" standalone="no"?>
        <xliff xmlns="urn:oasis:names:tc:xliff:document:1.2" version="1.2">
            <file additional-languages="fr" code="evolve-course-en" course-type="CourseEvolve" datatype="plaintext" evolve-draft-id="123abc" evolve-locale-id="" original="course[1]" rtl="false" source-language="en">
                <body>
                    <trans-unit datatype="plaintext" exceed-preview-link="https://www.pinterestacademy.com/student/enrollments/create_enrollment_from_token/8784da9d8339ece99c12d65c" id="course[2504103].name">
                        <source>Media Planner Practice Exam</source>
                        <target/>
                        <note>The name of the course</note>
                    </trans-unit>
                    <trans-unit datatype="html" exceed-preview-link="https://www.pinterestacademy.com/student/enrollments/create_enrollment_from_token/8784da9d8339ece99c12d65c" id="course[2504103].instructions">
                        <source>&lt;style&gt;&#13;
                .instructionspanel__photo {&#13;
                  border-radius: .8em;&#13;
                }&#13;
              &lt;/style&gt;</source>
                        <target/>
                        <note>Description shown in the catalog and at the top of the activity page</note>
                    </trans-unit>
                    <bin-unit exceed-preview-link="https://www.pinterestacademy.com/student/enrollments/create_enrollment_from_token/8784da9d8339ece99c12d65c" id="course[2504103].picture.target_url" mime-type="image/png" restype="uri">
                        <bin-source>
                            <external-file href="https://cdn.exceedlms.com/uploads/resource_course_pictures/targets/6658131/original/screenshot-2025-03-04-at-4-03-36-pm.png?Policy=eyJTdGF0ZW1lbnQiOlt7IlJlc291cmNlIjoiaHR0cHM6Ly9jZG4uZXhjZWVkbG1zLmNvbS91cGxvYWRzL3Jlc291cmNlX2NvdXJzZV9waWN0dXJlcy90YXJnZXRzLzY2NTgxMzEvb3JpZ2luYWwvc2NyZWVuc2hvdC0yMDI1LTAzLTA0LWF0LTQtMDMtMzYtcG0ucG5nIiwiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6eyJBV1M6RXBvY2hUaW1lIjoxNzQyOTI0MjE5fX19XX0_&amp;Signature=XvdkmMkmXtIqIyN6xQUJC7I5L7cCefGzmcg18bTV8SxqgX6i1QgJ-vnKWLZDTAxff8f-U2bTMo8EqljtjQyCyLBPJ40Fgpaf4TLGGgQsLrh4M~XD~5~ousow~~Y43Ui0K6MpriOCPXbzV5MycQ2Ngjp29QVkiwIcWNiaqhJ284OHwnTuv1tynClB8fhWVj1fX8ehLCVypaeVb8EFSFG2HG~W5p68h5Dk8la4NRV0ukfoZqEgiKecY3vF87sW6KWZU7KTnizeYOcX2IGCJmoYQpkoQMkkkgC1Q0nolw9JMNaCr~0o3XVaHiViKDqRnEr35yLRiKeL7nHh1wF~2g59zw__&amp;Key-Pair-Id=APKAJINUZDMKZJI5I6DA"/>
                        </bin-source>
                        <bin-target>
                            <external-file href=""/>
                        </bin-target>
                        <note>The course cover art</note>
                    </bin-unit>
        <trans-unit evolve-path="_videos.gifPlayButtonAriaLabel" evolve-preview-link="https://pinterestacademy.evolveauthoring.com/courses/67aa74c0fc16ab8fd188d3cd/preview/index.html?isShared=true&amp;contentId=67901d791b22e91f45a711c9" evolve-ref-id="67901d791b22e91f45a711c9" evolve-translation-type="String" evolve-type="course" id="335">
                        <source>start animation</source>
                        <target/>
                    </trans-unit>
                    <trans-unit evolve-path="_videos.gifStopButtonAriaLabel" evolve-preview-link="https://pinterestacademy.evolveauthoring.com/courses/67aa74c0fc16ab8fd188d3cd/preview/index.html?isShared=true&amp;contentId=67901d791b22e91f45a711c9" evolve-ref-id="67901d791b22e91f45a711c9" evolve-translation-type="String" evolve-type="course" id="336">
                        <source>stop animation</source>
                        <target/>
                    </trans-unit>
                </body>
            </file>
        </xliff>
        """);

    this.evolveService =
        new EvolveService(
            evolveConfigurationProperties,
            this.repositoryService,
            this.evolveClient,
            this.assetService,
            this.pollableTaskService,
            this.xliffUtils,
            this.branchRepository,
            this.branchStatisticRepository,
            this.assetContentRepository,
            this.tmService,
            this.branchService,
            this.assetExtractionByBranchRepository);
  }

  @Test
  public void testSyncWithOneReadyForTranslationCourseAndOneRepositoryLocale()
      throws RepositoryNameAlreadyUsedException, RepositoryLocaleCreationException {
    ZonedDateTime updatedOn = ZonedDateTime.now().minusDays(1);
    this.initData(updatedOn);
    Locale esLocale = this.localeService.findByBcp47Tag("es");
    RepositoryLocale esRepositoryLocale = new RepositoryLocale();
    esRepositoryLocale.setLocale(esLocale);

    Repository repository =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("test"),
            "",
            this.localeService.getDefaultLocale(),
            false,
            Sets.newHashSet(),
            Sets.newHashSet(esRepositoryLocale));

    this.evolveService.sync();

    assertEquals(updatedOn, this.evolveService.getEarliestUpdatedOn());

    verify(this.evolveClient)
        .startCourseTranslation(
            this.integerCaptor.capture(),
            this.stringCaptor.capture(),
            this.additionalLocalesCaptor.capture());

    assertEquals(1, (int) this.integerCaptor.getValue());
    assertEquals("es", this.stringCaptor.getValue());
    assertTrue(this.additionalLocalesCaptor.getValue().isEmpty());

    Branch branch = this.branchRepository.findByNameAndRepository("course_1", repository);
    assertNotNull(branch);

    this.branchStatisticService.computeAndSaveBranchStatistics(branch);

    BranchStatistic branchStatistic = this.branchStatisticRepository.findByBranch(branch);

    assertEquals(4, branchStatistic.getTotalCount());

    verify(this.evolveClient)
        .updateCourse(
            this.integerCaptor.capture(),
            this.translationStatusTypeCaptor.capture(),
            any(ZonedDateTime.class));

    assertEquals(1, (int) this.integerCaptor.getValue());
    assertEquals(IN_TRANSLATION, this.translationStatusTypeCaptor.getValue());
  }

  private void initData2(ZonedDateTime updatedOn) {
    CourseDTO courseDTO1 = new CourseDTO();
    courseDTO1.setId(1);
    courseDTO1.setTranslationStatus(IN_TRANSLATION);
    courseDTO1.setUpdatedOn(updatedOn);

    when(this.evolveClient.getCourses(any(CoursesGetRequest.class)))
        .thenReturn(Stream.of(courseDTO1));
    when(this.evolveClient.startCourseTranslation(anyInt(), anyString(), anySet()))
        .thenReturn(
            """
        <?xml version="1.0" encoding="UTF-8" standalone="no"?>
        <xliff xmlns="urn:oasis:names:tc:xliff:document:1.2" version="1.2">
            <file additional-languages="fr" code="evolve-course-en" course-type="CourseEvolve" datatype="plaintext" evolve-draft-id="123abc" evolve-locale-id="" original="course[1]" rtl="false" source-language="en">
                <body>
                    <trans-unit datatype="plaintext" exceed-preview-link="https://www.pinterestacademy.com/student/enrollments/create_enrollment_from_token/8784da9d8339ece99c12d65c" id="course[2504103].name">
                        <source>Media Planner Practice Exam</source>
                        <target/>
                        <note>The name of the course</note>
                    </trans-unit>
                    <trans-unit datatype="html" exceed-preview-link="https://www.pinterestacademy.com/student/enrollments/create_enrollment_from_token/8784da9d8339ece99c12d65c" id="course[2504103].instructions">
                        <source>&lt;style&gt;&#13;
                .instructionspanel__photo {&#13;
                  border-radius: .8em;&#13;
                }&#13;
              &lt;/style&gt;</source>
                        <target/>
                        <note>Description shown in the catalog and at the top of the activity page</note>
                    </trans-unit>
                    <bin-unit exceed-preview-link="https://www.pinterestacademy.com/student/enrollments/create_enrollment_from_token/8784da9d8339ece99c12d65c" id="course[2504103].picture.target_url" mime-type="image/png" restype="uri">
                        <bin-source>
                            <external-file href="https://cdn.exceedlms.com/uploads/resource_course_pictures/targets/6658131/original/screenshot-2025-03-04-at-4-03-36-pm.png?Policy=eyJTdGF0ZW1lbnQiOlt7IlJlc291cmNlIjoiaHR0cHM6Ly9jZG4uZXhjZWVkbG1zLmNvbS91cGxvYWRzL3Jlc291cmNlX2NvdXJzZV9waWN0dXJlcy90YXJnZXRzLzY2NTgxMzEvb3JpZ2luYWwvc2NyZWVuc2hvdC0yMDI1LTAzLTA0LWF0LTQtMDMtMzYtcG0ucG5nIiwiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6eyJBV1M6RXBvY2hUaW1lIjoxNzQyOTI0MjE5fX19XX0_&amp;Signature=XvdkmMkmXtIqIyN6xQUJC7I5L7cCefGzmcg18bTV8SxqgX6i1QgJ-vnKWLZDTAxff8f-U2bTMo8EqljtjQyCyLBPJ40Fgpaf4TLGGgQsLrh4M~XD~5~ousow~~Y43Ui0K6MpriOCPXbzV5MycQ2Ngjp29QVkiwIcWNiaqhJ284OHwnTuv1tynClB8fhWVj1fX8ehLCVypaeVb8EFSFG2HG~W5p68h5Dk8la4NRV0ukfoZqEgiKecY3vF87sW6KWZU7KTnizeYOcX2IGCJmoYQpkoQMkkkgC1Q0nolw9JMNaCr~0o3XVaHiViKDqRnEr35yLRiKeL7nHh1wF~2g59zw__&amp;Key-Pair-Id=APKAJINUZDMKZJI5I6DA"/>
                        </bin-source>
                        <bin-target>
                            <external-file href=""/>
                        </bin-target>
                        <note>The course cover art</note>
                    </bin-unit>
        <trans-unit evolve-path="_videos.gifPlayButtonAriaLabel" evolve-preview-link="https://pinterestacademy.evolveauthoring.com/courses/67aa74c0fc16ab8fd188d3cd/preview/index.html?isShared=true&amp;contentId=67901d791b22e91f45a711c9" evolve-ref-id="67901d791b22e91f45a711c9" evolve-translation-type="String" evolve-type="course" id="335">
                        <source>start animation</source>
                        <target/>
                    </trans-unit>
                    <trans-unit evolve-path="_videos.gifStopButtonAriaLabel" evolve-preview-link="https://pinterestacademy.evolveauthoring.com/courses/67aa74c0fc16ab8fd188d3cd/preview/index.html?isShared=true&amp;contentId=67901d791b22e91f45a711c9" evolve-ref-id="67901d791b22e91f45a711c9" evolve-translation-type="String" evolve-type="course" id="336">
                        <source>stop animation</source>
                        <target/>
                    </trans-unit>
                </body>
            </file>
        </xliff>
        """);

    this.evolveService =
        new EvolveService(
            evolveConfigurationProperties,
            this.repositoryService,
            this.evolveClient,
            this.assetService,
            this.pollableTaskService,
            this.xliffUtils,
            this.branchRepository,
            this.branchStatisticRepository,
            this.assetContentRepository,
            this.tmService,
            this.branchService,
            this.assetExtractionByBranchRepository);
  }

  @Test
  public void testSyncWithFullyTranslatedCourse()
      throws RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          UnsupportedAssetFilterTypeException,
          ExecutionException,
          InterruptedException {
    ZonedDateTime updatedOn = ZonedDateTime.now().minusDays(1);
    this.initData2(updatedOn);
    Locale esLocale = this.localeService.findByBcp47Tag("es");
    RepositoryLocale esRepositoryLocale = new RepositoryLocale();
    esRepositoryLocale.setLocale(esLocale);
    Repository repository =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("test"),
            "",
            this.localeService.getDefaultLocale(),
            false,
            Sets.newHashSet(),
            Sets.newHashSet(esRepositoryLocale));

    SourceAsset sourceAsset = new SourceAsset();
    sourceAsset.setBranch("course_" + 1);
    sourceAsset.setRepositoryId(repository.getId());
    sourceAsset.setPath(1 + ".xliff");
    sourceAsset.setContent(
        """
        <?xml version="1.0" encoding="UTF-8" standalone="no"?>
        <xliff xmlns="urn:oasis:names:tc:xliff:document:1.2" version="1.2">
            <file additional-languages="fr" code="evolve-course-en" course-type="CourseEvolve" datatype="plaintext" evolve-draft-id="123abc" evolve-locale-id="" original="course[1]" rtl="false" source-language="en">
                <body>
                    <trans-unit datatype="plaintext" exceed-preview-link="https://www.pinterestacademy.com/student/enrollments/create_enrollment_from_token/8784da9d8339ece99c12d65c" id="course[2504103].name">
                        <source>Media Planner Practice Exam</source>
                        <target/>
                        <note>The name of the course</note>
                    </trans-unit>
                    <trans-unit datatype="html" exceed-preview-link="https://www.pinterestacademy.com/student/enrollments/create_enrollment_from_token/8784da9d8339ece99c12d65c" id="course[2504103].instructions">
                        <source>&lt;style&gt;&#13;
                .instructionspanel__photo {&#13;
                  border-radius: .8em;&#13;
                }&#13;
              &lt;/style&gt;</source>
                        <target/>
                        <note>Description shown in the catalog and at the top of the activity page</note>
                    </trans-unit>
                    <bin-unit exceed-preview-link="https://www.pinterestacademy.com/student/enrollments/create_enrollment_from_token/8784da9d8339ece99c12d65c" id="course[2504103].picture.target_url" mime-type="image/png" restype="uri">
                        <bin-source>
                            <external-file href="https://cdn.exceedlms.com/uploads/resource_course_pictures/targets/6658131/original/screenshot-2025-03-04-at-4-03-36-pm.png?Policy=eyJTdGF0ZW1lbnQiOlt7IlJlc291cmNlIjoiaHR0cHM6Ly9jZG4uZXhjZWVkbG1zLmNvbS91cGxvYWRzL3Jlc291cmNlX2NvdXJzZV9waWN0dXJlcy90YXJnZXRzLzY2NTgxMzEvb3JpZ2luYWwvc2NyZWVuc2hvdC0yMDI1LTAzLTA0LWF0LTQtMDMtMzYtcG0ucG5nIiwiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6eyJBV1M6RXBvY2hUaW1lIjoxNzQyOTI0MjE5fX19XX0_&amp;Signature=XvdkmMkmXtIqIyN6xQUJC7I5L7cCefGzmcg18bTV8SxqgX6i1QgJ-vnKWLZDTAxff8f-U2bTMo8EqljtjQyCyLBPJ40Fgpaf4TLGGgQsLrh4M~XD~5~ousow~~Y43Ui0K6MpriOCPXbzV5MycQ2Ngjp29QVkiwIcWNiaqhJ284OHwnTuv1tynClB8fhWVj1fX8ehLCVypaeVb8EFSFG2HG~W5p68h5Dk8la4NRV0ukfoZqEgiKecY3vF87sW6KWZU7KTnizeYOcX2IGCJmoYQpkoQMkkkgC1Q0nolw9JMNaCr~0o3XVaHiViKDqRnEr35yLRiKeL7nHh1wF~2g59zw__&amp;Key-Pair-Id=APKAJINUZDMKZJI5I6DA"/>
                        </bin-source>
                        <bin-target>
                            <external-file href=""/>
                        </bin-target>
                        <note>The course cover art</note>
                    </bin-unit>
        <trans-unit evolve-path="_videos.gifPlayButtonAriaLabel" evolve-preview-link="https://pinterestacademy.evolveauthoring.com/courses/67aa74c0fc16ab8fd188d3cd/preview/index.html?isShared=true&amp;contentId=67901d791b22e91f45a711c9" evolve-ref-id="67901d791b22e91f45a711c9" evolve-translation-type="String" evolve-type="course" id="335">
                        <source>start animation</source>
                        <target/>
                    </trans-unit>
                    <trans-unit evolve-path="_videos.gifStopButtonAriaLabel" evolve-preview-link="https://pinterestacademy.evolveauthoring.com/courses/67aa74c0fc16ab8fd188d3cd/preview/index.html?isShared=true&amp;contentId=67901d791b22e91f45a711c9" evolve-ref-id="67901d791b22e91f45a711c9" evolve-translation-type="String" evolve-type="course" id="336">
                        <source>stop animation</source>
                        <target/>
                    </trans-unit>
                </body>
            </file>
        </xliff>
        """);

    String normalizedContent = NormalizationUtils.normalize(sourceAsset.getContent());
    PollableFuture<Asset> assetFuture =
        this.assetService.addOrUpdateAssetAndProcessIfNeeded(
            sourceAsset.getRepositoryId(),
            sourceAsset.getPath(),
            normalizedContent,
            sourceAsset.isExtractedContent(),
            sourceAsset.getBranch(),
            sourceAsset.getBranchCreatedByUsername(),
            sourceAsset.getBranchNotifiers(),
            null,
            sourceAsset.getFilterConfigIdOverride(),
            sourceAsset.getFilterOptions());

    sourceAsset.setAddedAssetId(assetFuture.get().getId());

    sourceAsset.setPollableTask(assetFuture.getPollableTask());

    this.pollableTaskService.waitForPollableTask(sourceAsset.getPollableTask().getId());

    TextUnitSearcherParameters textUnitSearcherParameters =
        new TextUnitSearcherParameters.Builder().repositoryId(repository.getId()).build();
    List<TextUnitDTO> textUnits = this.textUnitSearcher.search(textUnitSearcherParameters);

    textUnits.forEach(
        textUnitDTO -> {
          tmService.addTMTextUnitCurrentVariant(
              textUnitDTO.getTmTextUnitId(),
              esLocale.getId(),
              "Text",
              textUnitDTO.getTargetComment(),
              TMTextUnitVariant.Status.APPROVED,
              true);
        });

    Branch branch = this.branchRepository.findByNameAndRepository("course_1", repository);
    assertNotNull(branch);
    assertFalse(branch.getDeleted());

    this.branchStatisticService.computeAndSaveBranchStatistics(branch);

    this.evolveService.sync();

    branch = this.branchRepository.findByNameAndRepository("course_1", repository);
    assertTrue(branch.getDeleted());

    verify(this.evolveClient)
        .updateCourseTranslation(integerCaptor.capture(), stringCaptor.capture());

    assertEquals((int) integerCaptor.getValue(), 1);
    assertTrue(stringCaptor.getValue().contains("target-language=\"es\""));

    branch = this.branchRepository.findByNameAndRepository(null, repository);
    assertNotNull(branch);

    verify(this.evolveClient)
        .updateCourse(
            this.integerCaptor.capture(),
            this.translationStatusTypeCaptor.capture(),
            any(ZonedDateTime.class));

    assertEquals(1, (int) this.integerCaptor.getValue());
    assertEquals(TRANSLATED, this.translationStatusTypeCaptor.getValue());

    assertNotEquals(updatedOn, this.evolveService.getEarliestUpdatedOn());
  }

  @Test
  public void testSyncWithNotFullyTranslatedCourse()
      throws RepositoryNameAlreadyUsedException,
      RepositoryLocaleCreationException,
      UnsupportedAssetFilterTypeException,
      ExecutionException,
      InterruptedException {
    ZonedDateTime updatedOn = ZonedDateTime.now().minusDays(1);
    this.initData2(updatedOn);
    Locale esLocale = this.localeService.findByBcp47Tag("es");
    RepositoryLocale esRepositoryLocale = new RepositoryLocale();
    esRepositoryLocale.setLocale(esLocale);
    Repository repository =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("test"),
            "",
            this.localeService.getDefaultLocale(),
            false,
            Sets.newHashSet(),
            Sets.newHashSet(esRepositoryLocale));

    SourceAsset sourceAsset = new SourceAsset();
    sourceAsset.setBranch("course_" + 1);
    sourceAsset.setRepositoryId(repository.getId());
    sourceAsset.setPath(1 + ".xliff");
    sourceAsset.setContent(
        """
        <?xml version="1.0" encoding="UTF-8" standalone="no"?>
        <xliff xmlns="urn:oasis:names:tc:xliff:document:1.2" version="1.2">
            <file additional-languages="fr" code="evolve-course-en" course-type="CourseEvolve" datatype="plaintext" evolve-draft-id="123abc" evolve-locale-id="" original="course[1]" rtl="false" source-language="en">
                <body>
                    <trans-unit datatype="plaintext" exceed-preview-link="https://www.pinterestacademy.com/student/enrollments/create_enrollment_from_token/8784da9d8339ece99c12d65c" id="course[2504103].name">
                        <source>Media Planner Practice Exam</source>
                        <target/>
                        <note>The name of the course</note>
                    </trans-unit>
                    <trans-unit datatype="html" exceed-preview-link="https://www.pinterestacademy.com/student/enrollments/create_enrollment_from_token/8784da9d8339ece99c12d65c" id="course[2504103].instructions">
                        <source>&lt;style&gt;&#13;
                .instructionspanel__photo {&#13;
                  border-radius: .8em;&#13;
                }&#13;
              &lt;/style&gt;</source>
                        <target/>
                        <note>Description shown in the catalog and at the top of the activity page</note>
                    </trans-unit>
                    <bin-unit exceed-preview-link="https://www.pinterestacademy.com/student/enrollments/create_enrollment_from_token/8784da9d8339ece99c12d65c" id="course[2504103].picture.target_url" mime-type="image/png" restype="uri">
                        <bin-source>
                            <external-file href="https://cdn.exceedlms.com/uploads/resource_course_pictures/targets/6658131/original/screenshot-2025-03-04-at-4-03-36-pm.png?Policy=eyJTdGF0ZW1lbnQiOlt7IlJlc291cmNlIjoiaHR0cHM6Ly9jZG4uZXhjZWVkbG1zLmNvbS91cGxvYWRzL3Jlc291cmNlX2NvdXJzZV9waWN0dXJlcy90YXJnZXRzLzY2NTgxMzEvb3JpZ2luYWwvc2NyZWVuc2hvdC0yMDI1LTAzLTA0LWF0LTQtMDMtMzYtcG0ucG5nIiwiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6eyJBV1M6RXBvY2hUaW1lIjoxNzQyOTI0MjE5fX19XX0_&amp;Signature=XvdkmMkmXtIqIyN6xQUJC7I5L7cCefGzmcg18bTV8SxqgX6i1QgJ-vnKWLZDTAxff8f-U2bTMo8EqljtjQyCyLBPJ40Fgpaf4TLGGgQsLrh4M~XD~5~ousow~~Y43Ui0K6MpriOCPXbzV5MycQ2Ngjp29QVkiwIcWNiaqhJ284OHwnTuv1tynClB8fhWVj1fX8ehLCVypaeVb8EFSFG2HG~W5p68h5Dk8la4NRV0ukfoZqEgiKecY3vF87sW6KWZU7KTnizeYOcX2IGCJmoYQpkoQMkkkgC1Q0nolw9JMNaCr~0o3XVaHiViKDqRnEr35yLRiKeL7nHh1wF~2g59zw__&amp;Key-Pair-Id=APKAJINUZDMKZJI5I6DA"/>
                        </bin-source>
                        <bin-target>
                            <external-file href=""/>
                        </bin-target>
                        <note>The course cover art</note>
                    </bin-unit>
        <trans-unit evolve-path="_videos.gifPlayButtonAriaLabel" evolve-preview-link="https://pinterestacademy.evolveauthoring.com/courses/67aa74c0fc16ab8fd188d3cd/preview/index.html?isShared=true&amp;contentId=67901d791b22e91f45a711c9" evolve-ref-id="67901d791b22e91f45a711c9" evolve-translation-type="String" evolve-type="course" id="335">
                        <source>start animation</source>
                        <target/>
                    </trans-unit>
                    <trans-unit evolve-path="_videos.gifStopButtonAriaLabel" evolve-preview-link="https://pinterestacademy.evolveauthoring.com/courses/67aa74c0fc16ab8fd188d3cd/preview/index.html?isShared=true&amp;contentId=67901d791b22e91f45a711c9" evolve-ref-id="67901d791b22e91f45a711c9" evolve-translation-type="String" evolve-type="course" id="336">
                        <source>stop animation</source>
                        <target/>
                    </trans-unit>
                </body>
            </file>
        </xliff>
        """);

    String normalizedContent = NormalizationUtils.normalize(sourceAsset.getContent());
    PollableFuture<Asset> assetFuture =
        this.assetService.addOrUpdateAssetAndProcessIfNeeded(
            sourceAsset.getRepositoryId(),
            sourceAsset.getPath(),
            normalizedContent,
            sourceAsset.isExtractedContent(),
            sourceAsset.getBranch(),
            sourceAsset.getBranchCreatedByUsername(),
            sourceAsset.getBranchNotifiers(),
            null,
            sourceAsset.getFilterConfigIdOverride(),
            sourceAsset.getFilterOptions());

    sourceAsset.setAddedAssetId(assetFuture.get().getId());

    sourceAsset.setPollableTask(assetFuture.getPollableTask());

    this.pollableTaskService.waitForPollableTask(sourceAsset.getPollableTask().getId());

    Branch branch = this.branchRepository.findByNameAndRepository("course_1", repository);
    assertNotNull(branch);
    assertFalse(branch.getDeleted());

    this.branchStatisticService.computeAndSaveBranchStatistics(branch);

    this.evolveService.sync();

    branch = this.branchRepository.findByNameAndRepository("course_1", repository);
    assertFalse(branch.getDeleted());

    verify(this.evolveClient, times(0))
        .updateCourseTranslation(anyInt(), anyString());

    verify(this.evolveClient, times(0))
        .updateCourse(
            anyInt(),
            any(TranslationStatusType.class),
            any(ZonedDateTime.class));

    assertEquals(updatedOn, this.evolveService.getEarliestUpdatedOn());
  }

  @Test
  public void testSyncWithCourseWithoutTextUnits()
      throws RepositoryNameAlreadyUsedException,
      RepositoryLocaleCreationException,
      UnsupportedAssetFilterTypeException,
      ExecutionException,
      InterruptedException {
    ZonedDateTime updatedOn = ZonedDateTime.now().minusDays(1);
    this.initData2(updatedOn);
    Locale esLocale = this.localeService.findByBcp47Tag("es");
    RepositoryLocale esRepositoryLocale = new RepositoryLocale();
    esRepositoryLocale.setLocale(esLocale);
    Repository repository =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("test"),
            "",
            this.localeService.getDefaultLocale(),
            false,
            Sets.newHashSet(),
            Sets.newHashSet(esRepositoryLocale));

    SourceAsset sourceAsset = new SourceAsset();
    sourceAsset.setBranch("course_" + 1);
    sourceAsset.setRepositoryId(repository.getId());
    sourceAsset.setPath(1 + ".xliff");
    sourceAsset.setContent(
        """
        <?xml version="1.0" encoding="UTF-8" standalone="no"?>
        <xliff xmlns="urn:oasis:names:tc:xliff:document:1.2" version="1.2">
            <file additional-languages="fr" code="evolve-course-en" course-type="CourseEvolve" datatype="plaintext" evolve-draft-id="123abc" evolve-locale-id="" original="course[1]" rtl="false" source-language="en">
                <body>
                </body>
            </file>
        </xliff>
        """);

    String normalizedContent = NormalizationUtils.normalize(sourceAsset.getContent());
    PollableFuture<Asset> assetFuture =
        this.assetService.addOrUpdateAssetAndProcessIfNeeded(
            sourceAsset.getRepositoryId(),
            sourceAsset.getPath(),
            normalizedContent,
            sourceAsset.isExtractedContent(),
            sourceAsset.getBranch(),
            sourceAsset.getBranchCreatedByUsername(),
            sourceAsset.getBranchNotifiers(),
            null,
            sourceAsset.getFilterConfigIdOverride(),
            sourceAsset.getFilterOptions());

    sourceAsset.setAddedAssetId(assetFuture.get().getId());

    sourceAsset.setPollableTask(assetFuture.getPollableTask());

    this.pollableTaskService.waitForPollableTask(sourceAsset.getPollableTask().getId());

    Branch branch = this.branchRepository.findByNameAndRepository("course_1", repository);
    assertNotNull(branch);
    assertFalse(branch.getDeleted());

    this.branchStatisticService.computeAndSaveBranchStatistics(branch);

    this.evolveService.sync();

    branch = this.branchRepository.findByNameAndRepository("course_1", repository);
    assertTrue(branch.getDeleted());

    verify(this.evolveClient, times(0))
        .updateCourseTranslation(anyInt(), anyString());

    verify(this.evolveClient, times(0))
        .updateCourse(
            anyInt(),
            any(TranslationStatusType.class),
            any(ZonedDateTime.class));

    assertNotEquals(updatedOn, this.evolveService.getEarliestUpdatedOn());
  }

  private void initData3(ZonedDateTime updatedOn1, ZonedDateTime updatedOn2) {
    CourseDTO courseDTO1 = new CourseDTO();
    courseDTO1.setId(1);
    courseDTO1.setTranslationStatus(READY_FOR_TRANSLATION);
    courseDTO1.setUpdatedOn(updatedOn1);

    CourseDTO courseDTO2 = new CourseDTO();
    courseDTO2.setId(2);
    courseDTO2.setTranslationStatus(IN_TRANSLATION);
    courseDTO2.setUpdatedOn(updatedOn2);

    when(this.evolveClient.getCourses(any(CoursesGetRequest.class)))
        .thenReturn(Stream.of(courseDTO1, courseDTO2));
    when(this.evolveClient.startCourseTranslation(anyInt(), anyString(), anySet()))
        .thenReturn(
            """
        <?xml version="1.0" encoding="UTF-8" standalone="no"?>
        <xliff xmlns="urn:oasis:names:tc:xliff:document:1.2" version="1.2">
            <file additional-languages="fr" code="evolve-course-en" course-type="CourseEvolve" datatype="plaintext" evolve-draft-id="123abc" evolve-locale-id="" original="course[1]" rtl="false" source-language="en">
                <body>
                    <trans-unit datatype="plaintext" exceed-preview-link="https://www.pinterestacademy.com/student/enrollments/create_enrollment_from_token/8784da9d8339ece99c12d65c" id="course[2504103].name">
                        <source>Media Planner Practice Exam</source>
                        <target/>
                        <note>The name of the course</note>
                    </trans-unit>
                    <trans-unit datatype="html" exceed-preview-link="https://www.pinterestacademy.com/student/enrollments/create_enrollment_from_token/8784da9d8339ece99c12d65c" id="course[2504103].instructions">
                        <source>&lt;style&gt;&#13;
                .instructionspanel__photo {&#13;
                  border-radius: .8em;&#13;
                }&#13;
              &lt;/style&gt;</source>
                        <target/>
                        <note>Description shown in the catalog and at the top of the activity page</note>
                    </trans-unit>
                    <bin-unit exceed-preview-link="https://www.pinterestacademy.com/student/enrollments/create_enrollment_from_token/8784da9d8339ece99c12d65c" id="course[2504103].picture.target_url" mime-type="image/png" restype="uri">
                        <bin-source>
                            <external-file href="https://cdn.exceedlms.com/uploads/resource_course_pictures/targets/6658131/original/screenshot-2025-03-04-at-4-03-36-pm.png?Policy=eyJTdGF0ZW1lbnQiOlt7IlJlc291cmNlIjoiaHR0cHM6Ly9jZG4uZXhjZWVkbG1zLmNvbS91cGxvYWRzL3Jlc291cmNlX2NvdXJzZV9waWN0dXJlcy90YXJnZXRzLzY2NTgxMzEvb3JpZ2luYWwvc2NyZWVuc2hvdC0yMDI1LTAzLTA0LWF0LTQtMDMtMzYtcG0ucG5nIiwiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6eyJBV1M6RXBvY2hUaW1lIjoxNzQyOTI0MjE5fX19XX0_&amp;Signature=XvdkmMkmXtIqIyN6xQUJC7I5L7cCefGzmcg18bTV8SxqgX6i1QgJ-vnKWLZDTAxff8f-U2bTMo8EqljtjQyCyLBPJ40Fgpaf4TLGGgQsLrh4M~XD~5~ousow~~Y43Ui0K6MpriOCPXbzV5MycQ2Ngjp29QVkiwIcWNiaqhJ284OHwnTuv1tynClB8fhWVj1fX8ehLCVypaeVb8EFSFG2HG~W5p68h5Dk8la4NRV0ukfoZqEgiKecY3vF87sW6KWZU7KTnizeYOcX2IGCJmoYQpkoQMkkkgC1Q0nolw9JMNaCr~0o3XVaHiViKDqRnEr35yLRiKeL7nHh1wF~2g59zw__&amp;Key-Pair-Id=APKAJINUZDMKZJI5I6DA"/>
                        </bin-source>
                        <bin-target>
                            <external-file href=""/>
                        </bin-target>
                        <note>The course cover art</note>
                    </bin-unit>
        <trans-unit evolve-path="_videos.gifPlayButtonAriaLabel" evolve-preview-link="https://pinterestacademy.evolveauthoring.com/courses/67aa74c0fc16ab8fd188d3cd/preview/index.html?isShared=true&amp;contentId=67901d791b22e91f45a711c9" evolve-ref-id="67901d791b22e91f45a711c9" evolve-translation-type="String" evolve-type="course" id="335">
                        <source>start animation</source>
                        <target/>
                    </trans-unit>
                    <trans-unit evolve-path="_videos.gifStopButtonAriaLabel" evolve-preview-link="https://pinterestacademy.evolveauthoring.com/courses/67aa74c0fc16ab8fd188d3cd/preview/index.html?isShared=true&amp;contentId=67901d791b22e91f45a711c9" evolve-ref-id="67901d791b22e91f45a711c9" evolve-translation-type="String" evolve-type="course" id="336">
                        <source>stop animation</source>
                        <target/>
                    </trans-unit>
                </body>
            </file>
        </xliff>
        """);

    this.evolveService =
        new EvolveService(
            evolveConfigurationProperties,
            this.repositoryService,
            this.evolveClient,
            this.assetService,
            this.pollableTaskService,
            this.xliffUtils,
            this.branchRepository,
            this.branchStatisticRepository,
            this.assetContentRepository,
            this.tmService,
            this.branchService,
            this.assetExtractionByBranchRepository);
  }

  @Test
  public void testSyncWithTwoCoursesWithDifferentUpdatedOnDate() throws RepositoryNameAlreadyUsedException, RepositoryLocaleCreationException, UnsupportedAssetFilterTypeException, ExecutionException, InterruptedException {
    ZonedDateTime updatedOn1 = ZonedDateTime.now().minusDays(1);
    ZonedDateTime updatedOn2 = ZonedDateTime.now().minusDays(2);
    this.initData3(updatedOn1, updatedOn2);
    Locale esLocale = this.localeService.findByBcp47Tag("es");
    RepositoryLocale esRepositoryLocale = new RepositoryLocale();
    esRepositoryLocale.setLocale(esLocale);
    Repository repository =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("test"),
            "",
            this.localeService.getDefaultLocale(),
            false,
            Sets.newHashSet(),
            Sets.newHashSet(esRepositoryLocale));

    SourceAsset sourceAsset = new SourceAsset();
    sourceAsset.setBranch("course_" + 2);
    sourceAsset.setRepositoryId(repository.getId());
    sourceAsset.setPath(2 + ".xliff");
    sourceAsset.setContent(
        """
        <?xml version="1.0" encoding="UTF-8" standalone="no"?>
        <xliff xmlns="urn:oasis:names:tc:xliff:document:1.2" version="1.2">
            <file additional-languages="fr" code="evolve-course-en" course-type="CourseEvolve" datatype="plaintext" evolve-draft-id="123abc" evolve-locale-id="" original="course[1]" rtl="false" source-language="en">
                <body>
                    <trans-unit datatype="plaintext" exceed-preview-link="https://www.pinterestacademy.com/student/enrollments/create_enrollment_from_token/8784da9d8339ece99c12d65c" id="course[2504103].name">
                        <source>Media Planner Practice Exam</source>
                        <target/>
                        <note>The name of the course</note>
                    </trans-unit>
                    <trans-unit datatype="html" exceed-preview-link="https://www.pinterestacademy.com/student/enrollments/create_enrollment_from_token/8784da9d8339ece99c12d65c" id="course[2504103].instructions">
                        <source>&lt;style&gt;&#13;
                .instructionspanel__photo {&#13;
                  border-radius: .8em;&#13;
                }&#13;
              &lt;/style&gt;</source>
                        <target/>
                        <note>Description shown in the catalog and at the top of the activity page</note>
                    </trans-unit>
                    <bin-unit exceed-preview-link="https://www.pinterestacademy.com/student/enrollments/create_enrollment_from_token/8784da9d8339ece99c12d65c" id="course[2504103].picture.target_url" mime-type="image/png" restype="uri">
                        <bin-source>
                            <external-file href="https://cdn.exceedlms.com/uploads/resource_course_pictures/targets/6658131/original/screenshot-2025-03-04-at-4-03-36-pm.png?Policy=eyJTdGF0ZW1lbnQiOlt7IlJlc291cmNlIjoiaHR0cHM6Ly9jZG4uZXhjZWVkbG1zLmNvbS91cGxvYWRzL3Jlc291cmNlX2NvdXJzZV9waWN0dXJlcy90YXJnZXRzLzY2NTgxMzEvb3JpZ2luYWwvc2NyZWVuc2hvdC0yMDI1LTAzLTA0LWF0LTQtMDMtMzYtcG0ucG5nIiwiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6eyJBV1M6RXBvY2hUaW1lIjoxNzQyOTI0MjE5fX19XX0_&amp;Signature=XvdkmMkmXtIqIyN6xQUJC7I5L7cCefGzmcg18bTV8SxqgX6i1QgJ-vnKWLZDTAxff8f-U2bTMo8EqljtjQyCyLBPJ40Fgpaf4TLGGgQsLrh4M~XD~5~ousow~~Y43Ui0K6MpriOCPXbzV5MycQ2Ngjp29QVkiwIcWNiaqhJ284OHwnTuv1tynClB8fhWVj1fX8ehLCVypaeVb8EFSFG2HG~W5p68h5Dk8la4NRV0ukfoZqEgiKecY3vF87sW6KWZU7KTnizeYOcX2IGCJmoYQpkoQMkkkgC1Q0nolw9JMNaCr~0o3XVaHiViKDqRnEr35yLRiKeL7nHh1wF~2g59zw__&amp;Key-Pair-Id=APKAJINUZDMKZJI5I6DA"/>
                        </bin-source>
                        <bin-target>
                            <external-file href=""/>
                        </bin-target>
                        <note>The course cover art</note>
                    </bin-unit>
        <trans-unit evolve-path="_videos.gifPlayButtonAriaLabel" evolve-preview-link="https://pinterestacademy.evolveauthoring.com/courses/67aa74c0fc16ab8fd188d3cd/preview/index.html?isShared=true&amp;contentId=67901d791b22e91f45a711c9" evolve-ref-id="67901d791b22e91f45a711c9" evolve-translation-type="String" evolve-type="course" id="335">
                        <source>start animation</source>
                        <target/>
                    </trans-unit>
                    <trans-unit evolve-path="_videos.gifStopButtonAriaLabel" evolve-preview-link="https://pinterestacademy.evolveauthoring.com/courses/67aa74c0fc16ab8fd188d3cd/preview/index.html?isShared=true&amp;contentId=67901d791b22e91f45a711c9" evolve-ref-id="67901d791b22e91f45a711c9" evolve-translation-type="String" evolve-type="course" id="336">
                        <source>stop animation</source>
                        <target/>
                    </trans-unit>
                </body>
            </file>
        </xliff>
        """);

    String normalizedContent = NormalizationUtils.normalize(sourceAsset.getContent());
    PollableFuture<Asset> assetFuture =
        this.assetService.addOrUpdateAssetAndProcessIfNeeded(
            sourceAsset.getRepositoryId(),
            sourceAsset.getPath(),
            normalizedContent,
            sourceAsset.isExtractedContent(),
            sourceAsset.getBranch(),
            sourceAsset.getBranchCreatedByUsername(),
            sourceAsset.getBranchNotifiers(),
            null,
            sourceAsset.getFilterConfigIdOverride(),
            sourceAsset.getFilterOptions());

    sourceAsset.setAddedAssetId(assetFuture.get().getId());

    sourceAsset.setPollableTask(assetFuture.getPollableTask());

    this.pollableTaskService.waitForPollableTask(sourceAsset.getPollableTask().getId());

    Branch branch = this.branchRepository.findByNameAndRepository("course_2", repository);
    assertNotNull(branch);
    assertFalse(branch.getDeleted());

    this.branchStatisticService.computeAndSaveBranchStatistics(branch);

    this.evolveService.sync();

    branch = this.branchRepository.findByNameAndRepository("course_2", repository);
    assertFalse(branch.getDeleted());

    /*verify(this.evolveClient, times(0))
        .updateCourseTranslation(anyInt(), anyString());

    verify(this.evolveClient, times(0))
        .updateCourse(
            anyInt(),
            any(TranslationStatusType.class),
            any(ZonedDateTime.class));*/

    assertEquals(updatedOn2, this.evolveService.getEarliestUpdatedOn());
  }
}
