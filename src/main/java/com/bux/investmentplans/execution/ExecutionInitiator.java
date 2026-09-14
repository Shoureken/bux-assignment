package com.bux.investmentplans.execution;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer group of N instances processing {@code execution-triggers} in
 * parallel across partitions (1.6, 2.4). Scaling out is just adding
 * instances, up to the partition count — {@code concurrency} below is
 * per-instance thread concurrency, not a replacement for running more
 * instances.
 */
@Component
@RequiredArgsConstructor
class ExecutionInitiator {

    private final ExecutionCommandHandler commandHandler;

    @KafkaListener(
            topics = "${investment-plans.topics.execution-triggers}",
            groupId = "${investment-plans.execution-initiator.consumer-group}",
            concurrency = "${investment-plans.execution-initiator.concurrency}")
    void onExecutionDue(ExecutionDue due) {
        commandHandler.initiateExecution(due.planId(), due.date());
    }
}
