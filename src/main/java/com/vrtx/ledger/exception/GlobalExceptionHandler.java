package com.vrtx.ledger.exception;

import com.vrtx.ledger.common.ApiError;
import com.vrtx.ledger.transaction.Transaction;
import com.vrtx.ledger.transaction.TransactionRepository;
import com.vrtx.ledger.transaction.dto.TransactionResponse;
import com.vrtx.ledger.transaction.service.TransactionResponses;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final TransactionRepository transactionRepository;
    private final TransactionResponses transactionResponses;

    public GlobalExceptionHandler(TransactionRepository transactionRepository,
                                  TransactionResponses transactionResponses) {
        this.transactionRepository = transactionRepository;
        this.transactionResponses = transactionResponses;
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message, HttpServletRequest req) {
        return ResponseEntity.status(status)
                .body(ApiError.of(status.value(), code, message, req.getRequestURI()));
    }

    /**
     * Concurrent idempotency race: another request with the same key committed first.
     * This is NOT an error to the client - return the original transaction with 200,
     * exactly like a sequential replay. Status and body always agree.
     */
    @ExceptionHandler(DuplicateIdempotencyKeyException.class)
    public ResponseEntity<TransactionResponse> duplicateKey(DuplicateIdempotencyKeyException ex) {
        Transaction original = transactionRepository.findByIdempotencyKey(ex.getIdempotencyKey())
                .orElseThrow(() -> ex);
        return ResponseEntity.ok(transactionResponses.toResponse(original));
    }

    @ExceptionHandler({AccountNotFoundException.class, TransactionNotFoundException.class})
    public ResponseEntity<ApiError> notFound(RuntimeException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), req);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ApiError> insufficient(RuntimeException ex, HttpServletRequest req) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_FUNDS", ex.getMessage(), req);
    }

    @ExceptionHandler(RefundNotAllowedException.class)
    public ResponseEntity<ApiError> refund(RuntimeException ex, HttpServletRequest req) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "REFUND_NOT_ALLOWED", ex.getMessage(), req);
    }

    @ExceptionHandler(UnbalancedTransactionException.class)
    public ResponseEntity<ApiError> unbalanced(RuntimeException ex, HttpServletRequest req) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "UNBALANCED_TRANSACTION", ex.getMessage(), req);
    }

    @ExceptionHandler(CurrencyMismatchException.class)
    public ResponseEntity<ApiError> currency(RuntimeException ex, HttpServletRequest req) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "CURRENCY_MISMATCH", ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", msg, req);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiError> missingHeader(MissingRequestHeaderException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Missing required header: " + ex.getHeaderName(), req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> illegalArg(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> generic(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", ex.getMessage(), req);
    }
}
