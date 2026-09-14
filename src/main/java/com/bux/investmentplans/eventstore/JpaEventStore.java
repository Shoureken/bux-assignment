package com.bux.investmentplans.eventstore;

import com.bux.investmentplans.domain.events.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

/**
 * Straightforward implementation: for each event, insert one {@link EventRow}
 * (next sequence number for the aggregate) and one {@link OutboxRow} on the
 * {@code investment-plan-data} topic, all via plain repository calls — no
 * explicit transaction here, since callers (command handlers) already run
 * inside {@code @Transactional}, and this class only ever runs on that
 * thread.
 */
@Component
@RequiredArgsConstructor
class JpaEventStore implements EventStore {

    private final EventRowRepository eventRows;
    private final OutboxRepository outboxRows;
    private final ExecutionIdempotencyRepository idempotencyRows;
    private final EventPayloadCodec codec;

    @Override
    public void append(String aggregateId, List<DomainEvent> events) {
        long sequence = eventRows.countByAggregateId(aggregateId);
        for (DomainEvent event : events) {
            String payload = codec.serialize(event);
            sequence++;
            eventRows.save(EventRow.builder()
                    .aggregateId(aggregateId)
                    .sequence(sequence)
                    .eventType(event.getClass().getSimpleName())
                    .payload(payload)
                    .occurredAt(event.occurredAt())
                    .build());
            outboxRows.save(OutboxRow.builder()
                    .topic("investment-plan-data")
                    .messageKey(aggregateId)
                    .payload(payload)
                    .createdAt(event.occurredAt())
                    .build());
        }
    }

    @Override
    public List<DomainEvent> loadEvents(String aggregateId) {
        return eventRows.findByAggregateIdOrderBySequenceAsc(aggregateId).stream()
                .map(row -> codec.deserialize(row.getEventType(), row.getPayload()))
                .toList();
    }

    @Override
    public void appendOutboxMessage(String topic, String messageKey, Object payload) {
        outboxRows.save(OutboxRow.builder()
                .topic(topic)
                .messageKey(messageKey)
                .payload(codec.serializeAny(payload))
                .createdAt(Instant.now())
                .build());
    }

    @Override
    public boolean claimExecutionSlot(String planId, YearMonth executionMonth, String executionId) {
        try {
            // flush, not just save: the constraint violation must surface
            // here so the caller can catch it, not later at commit time.
            idempotencyRows.saveAndFlush(ExecutionIdempotencyRow.builder()
                    .planId(planId)
                    .executionMonth(executionMonth)
                    .executionId(executionId)
                    .build());
            return true;
        } catch (DataIntegrityViolationException alreadyClaimed) {
            return false;
        }
    }
}
