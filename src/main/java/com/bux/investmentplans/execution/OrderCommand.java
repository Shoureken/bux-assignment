package com.bux.investmentplans.execution;

import java.math.BigDecimal;

/**
 * Message shape sent to Trading/Orders on {@code order-commands} (1.6).
 * {@code correlationId} is {@code (executionId, assetId)} — Order's
 * identity (2.9) — so the fill/reject event on {@code order-data} can be
 * matched back without a separate generated order id.
 */
public record OrderCommand(String correlationId, String planId, String executionId, String assetId, BigDecimal quantity) {
}
