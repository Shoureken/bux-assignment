package com.bux.investmentplans.readmodel;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Backs the {@code orders} array in GET /plans/{id}/executions/{id} (1.4, 1.9). */
@Entity
@Table(name = "order_projection")
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA
@Builder
public class OrderProjection {

    @EmbeddedId
    private OrderProjectionId id;

    private BigDecimal quantity;

    private String status; // OrderStatus, as text
}
