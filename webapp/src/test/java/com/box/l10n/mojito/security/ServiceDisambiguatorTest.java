package com.box.l10n.mojito.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.box.l10n.mojito.entity.security.user.User;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(
    properties = {
      "app.name=InlineTestApplication",
      "l10n.spring.security.header.service.prefix=spiffe://"
    })
public class ServiceDisambiguatorTest {

  ServiceDisambiguator serviceDisambiguator = new ServiceDisambiguator();

  @Test
  public void testFindServiceWithShortestSharedAncestor_EdgeCases() {
    User sharedAncestor =
        serviceDisambiguator.findServiceWithCommonAncestor(
            new ArrayList<>(), "spiffe://test.com/container/service");
    assertNull(sharedAncestor);
    sharedAncestor = serviceDisambiguator.findServiceWithCommonAncestor(new ArrayList<>(), "");
    assertNull(sharedAncestor);
    sharedAncestor = serviceDisambiguator.findServiceWithCommonAncestor(new ArrayList<>(), null);
    assertNull(sharedAncestor);
    sharedAncestor = serviceDisambiguator.findServiceWithCommonAncestor(null, "");
    assertNull(sharedAncestor);
    sharedAncestor = serviceDisambiguator.findServiceWithCommonAncestor(null, "test");
    assertNull(sharedAncestor);
  }

  @Test
  public void testFindServiceWithShortestSharedAncestor_ExactMatch() throws Exception {
    List<User> services =
        generateServices(
            List.of(
                "spiffe://test.com/container2/tagService",
                "spiffe://test.com/container2/imageService",
                "spiffe://test.com/container2/userService",
                "spiffe://test.com/container1/tagService",
                "spiffe://test.com/container1/imageService",
                "spiffe://test.com/container1/userService"));
    User result =
        serviceDisambiguator.findServiceWithCommonAncestor(
            services, "spiffe://test.com/container1/tagService");
    assertEquals(result, services.get(3));
    result =
        serviceDisambiguator.findServiceWithCommonAncestor(
            services, "spiffe://test.com/container1/imageService");
    assertEquals(result, services.get(4));
  }

  @Test
  public void testFindServiceWithShortestSharedAncestor_SpecificExamples() {
    List<User> services =
        generateServices(
            List.of(
                "spiffe://test.com/infra/jenkins/agent1",
                "spiffe://test.com/infra/jenkins/agent2",
                "spiffe://test.com/infra/jenkins"));
    User result =
        serviceDisambiguator.findServiceWithCommonAncestor(
            services, "spiffe://test.com/infra/jenkins/agent1");
    assertEquals(result, services.getFirst());
    result =
        serviceDisambiguator.findServiceWithCommonAncestor(
            services, "spiffe://test.com/infra/jenkins/agent3");
    assertEquals(result, services.get(2));
    result = serviceDisambiguator.findServiceWithCommonAncestor(services, "spiffe://test.com");
    assertNull(result);
  }

  private List<User> generateServices(List<String> names) {
    return IntStream.range(0, names.size())
        .mapToObj(
            i -> {
              String serviceName = names.get(i);
              User user = new User();
              user.setUsername(serviceName);
              user.setId((long) i);
              return user;
            })
        .toList();
  }
}
