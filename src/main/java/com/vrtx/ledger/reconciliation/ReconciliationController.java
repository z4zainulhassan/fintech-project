package com.vrtx.ledger.reconciliation;

import com.vrtx.ledger.reconciliation.dto.ReconciliationReport;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reconciliation")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @GetMapping
    public ReconciliationReport reconcile() {
        return reconciliationService.run();
    }
}
