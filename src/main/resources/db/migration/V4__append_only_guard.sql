-- Defence-in-depth: enforce immutability at the database level, not just in code.
-- The ledger physically cannot be updated or deleted.
CREATE OR REPLACE FUNCTION reject_ledger_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'ledger_entries is append-only: % is not allowed', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ledger_entries_no_update
    BEFORE UPDATE OR DELETE ON ledger_entries
    FOR EACH ROW EXECUTE FUNCTION reject_ledger_mutation();

-- Same guard for transactions: they are immutable once written.
CREATE TRIGGER trg_transactions_no_update
    BEFORE UPDATE OR DELETE ON transactions
    FOR EACH ROW EXECUTE FUNCTION reject_ledger_mutation();
