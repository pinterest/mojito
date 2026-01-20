package com.box.l10n.mojito.rest.repository;

import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.aspect.StopWatch;
import com.box.l10n.mojito.service.branch.BranchService;
import com.box.l10n.mojito.service.branch.BranchTextUnitStatusDTO;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BranchWS {

  /** logger */
  static Logger logger = getLogger(BranchWS.class);

  @Autowired private BranchService branchService;

  @Operation(summary = "Get Branch TextUnit translation status")
  @RequestMapping(value = "/api/branch/textUnitStatus", method = RequestMethod.GET)
  @StopWatch
  public BranchTextUnitStatusDTO getTextUnitStatus(
      @RequestParam(value = "branchId") Long branchId) {
    return branchService.getBranchTextUnitStatuses(branchId);
  }
}
