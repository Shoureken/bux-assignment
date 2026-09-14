package com.bux.investmentplans.eventstore;

import com.bux.investmentplans.domain.events.DomainEvent;
import org.springframework.stereotype.Component;

/**
 * JSON (de)serialization for event payloads. Elided here — a real
 * implementation is a thin Jackson {@code ObjectMapper} wrapper that knows
 * how to pick the concrete {@link DomainEvent} subtype from {@code
 * eventType} (e.g. a Jackson subtype registry keyed by simple class name).
 */
@Component
class EventPayloadCodec {

    String serialize(DomainEvent event) {
        throw new UnsupportedOperationException("pseudocode — Jackson ObjectMapper.writeValueAsString(event)");
    }

    DomainEvent deserialize(String eventType, String payload) {
        throw new UnsupportedOperationException("pseudocode — dispatch on eventType, then ObjectMapper.readValue(payload, concreteType)");
    }

    String serializeAny(Object payload) {
        throw new UnsupportedOperationException("pseudocode — ObjectMapper.writeValueAsString(payload)");
    }
}
