package com.bux.investmentplans.projection;

import com.bux.investmentplans.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Verifies the read side (docs/detailed/1-architecture-overview.md 1.3-1.4):
 * publishing a {@code PlanCreated} on {@code investment-plan-data} — what
 * Debezium would do after a command handler commits — eventually produces
 * a matching {@code plan_projection} row. Publishes the raw JSON directly
 * with a {@code KafkaTemplate<String, String>} rather than going through
 * the Outbox/Debezium hop, since that hop is shared infra, not this
 * module's code.
 */
class PlanProjectionWorkerIT extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${investment-plans.topics.domain-events}")
    private String domainEventsTopic;

    @Test
    void planCreatedEvent_isProjectedIntoReadModel() {
        String planId = UUID.randomUUID().toString();
        // In a real run this is exactly what JpaEventStore.append() wrote to
        // the outbox row's payload column (event-type dispatch elided, see
        // eventstore.EventPayloadCodec).
        String planCreatedJson = """
                {"eventType":"PlanCreated","aggregateId":"%s","name":"Monthly ETF buy",
                 "recurrenceDay":15,"investments":[{"assetId":"AAPL","quantity":10}]}
                """.formatted(planId);

        kafkaTemplate.send(domainEventsTopic, planId, planCreatedJson);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Integer rows = jdbcTemplate.queryForObject(
                    "select count(*) from plan_projection where id = ?", Integer.class, planId);
            assertThat(rows).isEqualTo(1);
        });
    }
}
