package com.bux.investmentplans.api;

import com.bux.investmentplans.domain.Investment;
import com.bux.investmentplans.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the transactional-outbox guarantee from
 * docs/detailed/2-key-design-decisions.md 2.2: a successful command leaves
 * exactly one Event Store row and one matching Outbox row, written
 * together. Asserts against the tables directly (rather than through
 * package-private repositories) since that's the actual contract Debezium
 * relies on.
 */
class PlanCommandHandlerIT extends AbstractIntegrationTest {

    @Autowired
    private PlanCommandHandler commandHandler;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createPlan_appendsEventAndOutboxRowInOneTransaction() {
        List<Investment> investments = List.of(
                new Investment("AAPL", BigDecimal.TEN),
                new Investment("MSFT", BigDecimal.valueOf(5))
        );

        String planId = commandHandler.createPlan("Monthly ETF buy", 15, investments);

        Integer eventRows = jdbcTemplate.queryForObject(
                "select count(*) from event_store where aggregate_id = ? and event_type = 'PlanCreated'",
                Integer.class, planId);
        Integer outboxRows = jdbcTemplate.queryForObject(
                "select count(*) from outbox where message_key = ? and topic = 'investment-plan-data'",
                Integer.class, planId);

        assertThat(eventRows).isEqualTo(1);
        assertThat(outboxRows).isEqualTo(1);
    }
}
