package com.bux.investmentplans.readmodel;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-model row queried directly by GET /plans and by the Execution
 * Dispatcher's due-plan scan (docs/detailed/1-architecture-overview.md 1.3-1.5).
 * Built and kept up to date only by the Projection Worker — never written by
 * a command handler.
 */
@Entity
@Table(name = "plan_projection")
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA
@Builder
public class PlanProjection {

    @Id
    private String id;

    private String name;

    @Column(name = "status")
    private String status; // PlanStatus, stored as text for read-side simplicity

    @Column(name = "recurrence_day")
    private int recurrenceDay;

    @ElementCollection
    @CollectionTable(name = "plan_projection_investment", joinColumns = @JoinColumn(name = "plan_id"))
    @Builder.Default
    private List<InvestmentProjection> investments = new ArrayList<>();
}
