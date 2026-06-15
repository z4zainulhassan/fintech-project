package com.vrtx.ledger.account;

import com.vrtx.ledger.account.dto.LedgerEntryView;
import com.vrtx.ledger.account.dto.LedgerResponse;
import com.vrtx.ledger.exception.AccountNotFoundException;
import com.vrtx.ledger.ledger.LedgerEntryRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AccountController {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public AccountController(AccountRepository accountRepository,
                            LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @GetMapping("/accounts/{id}/ledger")
    @Transactional(readOnly = true)
    public LedgerResponse ledger(@PathVariable Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        long balance = ledgerEntryRepository.balanceOf(id);
        List<LedgerEntryView> entries = ledgerEntryRepository.findByAccountIdOrderByIdAsc(id)
                .stream().map(LedgerEntryView::from).toList();
        return new LedgerResponse(account.getId(), account.getType().name(),
                account.getCurrency(), balance, entries);
    }
}
