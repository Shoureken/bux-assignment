package com.bux.investmentplans.orderstatus;

/**
 * Assumed shape of Trading/Orders' {@code order-data} events (1.7). {@code
 * correlationId} echoes back the {@code (executionId, assetId)} pair we
 * sent in the order command (2.9). {@code permanent} on rejection is an
 * open assumption — see docs/detailed/5-assumptions-and-open-questions.md
 * 5.2 #7: we need Trading/Orders to expose a reason code that distinguishes
 * "this asset can never be traded" from "try again next time."
 */
public sealed interface OrderResultMessage {

    String correlationId();

    record OrderFilled(String correlationId) implements OrderResultMessage {
    }

    record OrderRejected(String correlationId, String reasonCode, boolean permanent) implements OrderResultMessage {
    }
}
