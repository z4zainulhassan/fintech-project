package com.vrtx.ledger.transaction;

import com.vrtx.ledger.transaction.dto.*;
import com.vrtx.ledger.transaction.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final PaymentService paymentService;
    private final RefundService refundService;
    private final TransferService transferService;
    private final SettlementService settlementService;
    private final TransactionResponses responses;
    private final TransactionRepository transactionRepository;

    public TransactionController(PaymentService paymentService, RefundService refundService,
                                 TransferService transferService, SettlementService settlementService,
                                 TransactionResponses responses, TransactionRepository transactionRepository) {
        this.paymentService = paymentService;
        this.refundService = refundService;
        this.transferService = transferService;
        this.settlementService = settlementService;
        this.responses = responses;
        this.transactionRepository = transactionRepository;
    }

    @PostMapping("/payment")
    public ResponseEntity<TransactionResponse> payment(
            @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody PaymentRequest request) {
        return respond(paymentService.pay(request, key));
    }

    @PostMapping("/refund")
    public ResponseEntity<TransactionResponse> refund(
            @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody RefundRequest request) {
        return respond(refundService.refund(request, key));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody TransferRequest request) {
        return respond(transferService.transfer(request, key));
    }

    @PostMapping("/settlement")
    public ResponseEntity<TransactionResponse> settlement(
            @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody SettlementRequest request) {
        return respond(settlementService.settle(request, key));
    }

    @GetMapping("/{id}")
    public TransactionResponse get(@PathVariable Long id) {
        Transaction txn = transactionRepository.findById(id)
                .orElseThrow(() -> new com.vrtx.ledger.exception.TransactionNotFoundException(id));
        return responses.toResponse(txn);
    }

    /** 201 when a new transaction is created, 200 when an idempotent replay returns the original. */
    private ResponseEntity<TransactionResponse> respond(PostingOutcome outcome) {
        return ResponseEntity
                .status(outcome.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(outcome.response());
    }
}
