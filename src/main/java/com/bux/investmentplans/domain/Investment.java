package com.bux.investmentplans.domain;

/** One asset in a Plan: how much to buy of {@code assetId} on each execution. */
public record Investment(String assetId, java.math.BigDecimal quantity) {
}
