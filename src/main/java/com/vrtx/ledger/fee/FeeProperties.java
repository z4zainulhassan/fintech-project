package com.vrtx.ledger.fee;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** configurable fees. Bound from ledger.fees.* in application.yml. */
@ConfigurationProperties(prefix = "ledger.fees")
public class FeeProperties {

    /** Default fee rate in basis points (100 bps = 1%). Example default 300 = 3%. */
    private int defaultRateBps = 300;

    public int getDefaultRateBps() { return defaultRateBps; }
    public void setDefaultRateBps(int defaultRateBps) { this.defaultRateBps = defaultRateBps; }
}
