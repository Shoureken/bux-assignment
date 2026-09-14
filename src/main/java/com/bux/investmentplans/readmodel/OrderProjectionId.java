package com.bux.investmentplans.readmodel;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** Mirrors the domain: (executionId, assetId) is an Order's identity (2.9) — no separate generated id. */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class OrderProjectionId implements Serializable {

    private String executionId;
    private String assetId;
}
