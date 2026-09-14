package com.bux.investmentplans.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code investment-plans:} tree in application.yml. Preferred
 * over scattering {@code @Value} across constructor parameters — besides
 * being the more idiomatic Spring Boot style, {@code @Value} on a
 * Lombok-generated constructor parameter doesn't work out of the box
 * (Lombok doesn't copy per-field annotations onto the constructor unless
 * configured to; a single injected properties object avoids the issue).
 */
@ConfigurationProperties(prefix = "investment-plans")
public record InvestmentPlansProperties(
        Topics topics,
        ExecutionDispatcher executionDispatcher,
        ExecutionInitiator executionInitiator
) {

    public record Topics(
            String domainEvents,
            String orderCommands,
            String orderData,
            String schedulerTick,
            String executionTriggers
    ) {
    }

    public record ExecutionDispatcher(int scanPageSize) {
    }

    public record ExecutionInitiator(String consumerGroup, int concurrency) {
    }
}
