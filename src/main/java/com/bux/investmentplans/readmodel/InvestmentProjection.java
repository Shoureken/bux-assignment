package com.bux.investmentplans.readmodel;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Read-side copy of {@code domain.Investment}, embedded in {@link PlanProjection}. */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentProjection {

    private String assetId;
    private BigDecimal quantity;
}
