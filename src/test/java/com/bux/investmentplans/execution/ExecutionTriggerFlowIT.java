package com.bux.investmentplans.execution;

import com.bux.investmentplans.api.PlanCommandHandler;
import com.bux.investmentplans.domain.Investment;
import com.bux.investmentplans.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the idempotency guarantee from
 * docs/detailed/2-key-design-decisions.md 2.5: two {@code InitiateExecution}
 * calls for the same plan and month — modelling a redelivered {@code
 * ExecutionDue} — result in exactly one claimed slot and one Execution.
 */
class ExecutionTriggerFlowIT extends AbstractIntegrationTest {

    @Autowired
    private PlanCommandHandler planCommandHandler;

    @Autowired
    private ExecutionCommandHandler executionCommandHandler;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void duplicateInitiateExecution_isANoOp() {
        String planId = planCommandHandler.createPlan(
                "Monthly ETF buy", 15, List.of(new Investment("AAPL", BigDecimal.TEN)));
        LocalDate executionDay = LocalDate.of(2026, 9, 15);

        executionCommandHandler.initiateExecution(planId, executionDay);
        // Simulates a redelivered ExecutionDue for the same plan/month —
        // e.g. a consumer-group rebalance before the offset committed.
        executionCommandHandler.initiateExecution(planId, executionDay);

        Integer claimedSlots = jdbcTemplate.queryForObject(
                "select count(*) from execution_idempotency where plan_id = ?", Integer.class, planId);
        Integer executionsInitiated = jdbcTemplate.queryForObject(
                "select count(*) from event_store where event_type = 'ExecutionInitiated' and payload like ?",
                Integer.class, "%" + planId + "%");

        assertThat(claimedSlots).isEqualTo(1);
        assertThat(executionsInitiated).isEqualTo(1);
    }
}
