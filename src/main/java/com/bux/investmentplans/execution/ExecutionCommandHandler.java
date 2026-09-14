package com.bux.investmentplans.execution;

import com.bux.investmentplans.config.InvestmentPlansProperties;
import com.bux.investmentplans.domain.Execution;
import com.bux.investmentplans.domain.Investment;
import com.bux.investmentplans.domain.Plan;
import com.bux.investmentplans.eventstore.EventStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Handles {@code InitiateExecution} (1.6, 2.5-2.6). The idempotency claim
 * and the event appends happen in one transaction: either both commit, or a
 * duplicate claim short-circuits before anything is appended.
 */
@Service
@RequiredArgsConstructor
class ExecutionCommandHandler {

    private final EventStore eventStore;
    private final InvestmentPlansProperties properties;

    @Transactional
    void initiateExecution(String planId, LocalDate date) {
        YearMonth executionMonth = YearMonth.from(date);
        String executionId = UUID.randomUUID().toString();

        boolean claimed = eventStore.claimExecutionSlot(planId, executionMonth, executionId);
        if (!claimed) {
            // Another delivery (retry, rebalance, redelivered ExecutionDue)
            // already initiated this plan's execution for this month —
            // no-op, by design (2.5).
            return;
        }

        Plan plan = Plan.replay(planId, eventStore.loadEvents(planId));
        List<Investment> investments = plan.investments(); // snapshot taken now, execution is immutable after (2.6)

        var events = Execution.decideInitiate(executionId, planId, date, investments);
        eventStore.append(executionId, events);

        String orderCommandsTopic = properties.topics().orderCommands();
        for (Investment investment : investments) {
            String correlationId = executionId + ":" + investment.assetId();
            var command = new OrderCommand(correlationId, planId, executionId, investment.assetId(), investment.quantity());
            eventStore.appendOutboxMessage(orderCommandsTopic, correlationId, command);
        }
    }
}
