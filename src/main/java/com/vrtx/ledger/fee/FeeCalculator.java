package com.vrtx.ledger.fee;

import org.springframework.stereotype.Component;

@Component
public class FeeCalculator {

    private final FeeProperties props;

    public FeeCalculator(FeeProperties props) {
        this.props = props;
    }

    /**
     * Resolve the fee for a payment.
     * - If the client supplies an explicit feeAmount, use it (client override).
     * - Otherwise derive it from the configured rate: amount * bps / 10000 (floor).
     */
    public long resolveFee(long amount, Long explicitFee) {
        if (explicitFee != null) {
            if (explicitFee < 0) throw new IllegalArgumentException("feeAmount must be >= 0");
            if (explicitFee >= amount) throw new IllegalArgumentException("feeAmount must be < amount");
            return explicitFee;
        }
        return Math.floorDiv(amount * props.getDefaultRateBps(), 10000L);
    }
}
