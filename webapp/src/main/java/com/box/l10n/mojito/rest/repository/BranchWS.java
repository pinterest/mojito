package com.box.l10n.mojito.rest.repository;

import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.aspect.StopWatch;
import com.box.l10n.mojito.service.branch.BranchNotFoundException;
import com.box.l10n.mojito.service.branch.BranchService;
import com.box.l10n.mojito.service.branch.BranchTextUnitStatusDTO;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class BranchWS {

  /** logger */
  static Logger logger = getLogger(BranchWS.class);

  private final BranchService branchService;

  public BranchWS(BranchService branchService) {
    this.branchService = branchService;
  }

  @Operation(summary = "Get a Branch's TextUnit statuses")
  @RequestMapping(value = "/api/branch/textUnitStatus", method = RequestMethod.GET)
  @StopWatch
  public BranchTextUnitStatusDTO getTextUnitStatus(
      @RequestParam(value = "branchName") String branchName,
      @RequestParam(value = "repositoryName") String repoName) {

    try {
      return branchService.getBranchTextUnitStatuses(branchName, repoName);
    } catch (BranchNotFoundException branchNotFoundException) {
      logger.info(branchNotFoundException.getMessage(), branchNotFoundException);
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, branchNotFoundException.getMessage(), branchNotFoundException);
    }
  }
}
