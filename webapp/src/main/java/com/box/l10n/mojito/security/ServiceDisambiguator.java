package com.box.l10n.mojito.security;

import com.box.l10n.mojito.entity.security.user.User;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty("l10n.spring.security.services.enableFuzzyMatch")
public class ServiceDisambiguator {
  @Autowired HeaderSecurityConfig headerSecurityConfig;

  /***
   * This method finds the service with the longest shared path (basically a parent in path directory)
   * The service uses its parents user if it exists. If the exact service exists, then that user is
   * used instead of the parent; to allow for children services to have different permissions
   *
   * @param services list of relevant services
   * @return the User or null if none match
   */
  public User findServiceWithCommonAncestor(List<User> services, String servicePath) {
    if (services == null || servicePath == null || services.isEmpty() || servicePath.isEmpty()) {
      return null;
    }

    String[] servicePathElements = servicePath.split(headerSecurityConfig.serviceDelimiter);
    String longestAncestorPath = null;
    User closestService = null;

    for (User currentService : services) {
      String currentServicePath = currentService.getUsername();
      // Check for exact match first
      if (servicePath.equals(currentServicePath)) {
        return currentService;
      }

      String[] currentPathElements =
          currentServicePath.split(headerSecurityConfig.serviceDelimiter);
      StringBuilder commonAncestor = new StringBuilder();

      int minLength = Math.min(servicePathElements.length, currentPathElements.length);

      for (int i = 0; i < minLength; i++) {
        if (servicePathElements[i].equals(currentPathElements[i])) {
          commonAncestor.append(servicePathElements[i]);
          if (i < minLength - 1) {
            commonAncestor.append(headerSecurityConfig.serviceDelimiter);
          }
        }
      }

      // Update the service which shares the shortest path
      String mostCommonAncestor = commonAncestor.toString();
      if (mostCommonAncestor.isEmpty()) {
        continue;
      }

      if (mostCommonAncestor.length() != currentServicePath.length()) {
        continue;
      }

      if (longestAncestorPath == null
          || mostCommonAncestor.length() > longestAncestorPath.length()) {
        longestAncestorPath = mostCommonAncestor;
        closestService = currentService;
      }
    }

    return closestService;
  }
}
