package com.bux.investmentplans.readmodel;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Backs GET /plans/{id}/executions[/{id}] (1.4). */
@Entity
@Table(name = "execution_projection")
@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA
@Builder
public class ExecutionProjection {

    @Id
    private String id;

    @Column(name = "plan_id")
    private String planId;

    @Column(name = "execution_day")
    private LocalDate executionDay;

    @Builder.Default
    private String status = "INITIATED"; // ExecutionStatus, as text

    @Column(name = "filled_count")
    private int filledCount;

    @Column(name = "rejected_count")
    private int rejectedCount;

    public void markProcessed(int filledCount, int rejectedCount) {
        this.status = "PROCESSED";
        this.filledCount = filledCount;
        this.rejectedCount = rejectedCount;
    }
}
