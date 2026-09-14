package com.bux.investmentplans.projection;

import com.bux.investmentplans.domain.events.DomainEvent;
import org.springframework.stereotype.Component;

/**
 * Decodes the JSON payload Debezium hands the Kafka consumer back into a
 * {@link DomainEvent}. Elided (see {@code eventstore.EventPayloadCodec} for
 * the write-side equivalent) — a real implementation needs the event type
 * from the Kafka record header/key to pick the concrete subtype.
 */
@Component
class ProjectionCodec {

    DomainEvent decode(String rawPayload) {
        throw new UnsupportedOperationException("pseudocode — ObjectMapper.readValue keyed by event-type header");
    }
}
