package com.vrtx.ledger.account;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Lock the given account rows FOR UPDATE, always in ascending id order.
     * Consistent lock ordering is what prevents deadlocks when two concurrent
     * transactions touch the same pair of accounts in opposite directions.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id in :ids order by a.id")
    List<Account> lockByIds(@Param("ids") List<Long> ids);
}
