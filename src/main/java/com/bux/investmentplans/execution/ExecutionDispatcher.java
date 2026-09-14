package com.bux.investmentplans.execution;

import com.bux.investmentplans.config.InvestmentPlansProperties;
import com.bux.investmentplans.readmodel.PlanProjection;
import com.bux.investmentplans.readmodel.PlanReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Consumes the daily {@code scheduler-tick} and fans out one {@code
 * ExecutionDue} per due plan (docs/detailed/1-architecture-overview.md 1.5,
 * 2.4). Read-only — no DB write here, no Outbox involved (see 1.5's "Why no
 * Outbox here"): a crash mid-scan just means the tick is redelivered and the
 * whole scan restarts, which is safe because {@code InitiateExecution}
 * downstream is idempotent (2.5).
 */
@Component
@RequiredArgsConstructor
class ExecutionDispatcher {

    private final PlanReadRepository planReads;
    private final KafkaTemplate<String, ExecutionDue> kafkaTemplate;
    private final InvestmentPlansProperties properties;

    @KafkaListener(topics = "${investment-plans.topics.scheduler-tick}", groupId = "investment-plans.execution-dispatcher")
    void onDayTick(LocalDate date) {
        int day = date.getDayOfMonth();
        int pageSize = properties.executionDispatcher().scanPageSize();
        String executionTriggersTopic = properties.topics().executionTriggers();
        Optional<String> afterId = Optional.empty();

        List<PlanProjection> page;
        do {
            page = planReads.findDuePlansPage(day, afterId, PageRequest.of(0, pageSize));
            for (PlanProjection plan : page) {
                // Keyed by plan_id so all triggers for one plan land on the
                // same partition — not that it matters here, since each
                // plan is due at most once a day, but it keeps ordering
                // well-defined if that ever changes.
                kafkaTemplate.send(executionTriggersTopic, plan.getId(), new ExecutionDue(plan.getId(), date));
            }
            if (!page.isEmpty()) {
                afterId = Optional.of(page.get(page.size() - 1).getId());
            }
        } while (page.size() == pageSize);
    }
}
