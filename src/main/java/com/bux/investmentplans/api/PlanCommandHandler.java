package com.bux.investmentplans.api;

import com.bux.investmentplans.domain.Investment;
import com.bux.investmentplans.domain.Plan;
import com.bux.investmentplans.domain.events.PlanCreated;
import com.bux.investmentplans.eventstore.EventStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Write path for Plan commands (docs/detailed/1-architecture-overview.md
 * 1.3, 2.2). Deliberately takes plain domain types, not the generated API
 * models — {@link PlanController} (and its MapStruct mapper) own the
 * translation from HTTP request to these arguments, so this class has no
 * dependency on the OpenAPI-generated layer.
 *
 * <p>The whole point of {@code @Transactional} here: {@link
 * EventStore#append} inserts both the event-store row and the outbox row —
 * if this method throws after that call, both roll back together, so a
 * validation failure downstream can never leave a half-written event.
 */
@Service
@RequiredArgsConstructor
public class PlanCommandHandler {

    private final EventStore eventStore;

    @Transactional
    public String createPlan(String name, int recurrenceDay, List<Investment> investments) {
        String planId = UUID.randomUUID().toString();
        PlanCreated event = Plan.decideCreate(planId, name, recurrenceDay, investments);

        // append() persists the event AND writes the matching outbox row in
        // one call, on this method's transaction — see EventStore's javadoc.
        eventStore.append(planId, List.of(event));
        return planId;
    }

    @Transactional
    public void editPlan(String planId, String name, int recurrenceDay, List<Investment> investments) {
        Plan plan = Plan.replay(planId, eventStore.loadEvents(planId));
        var event = plan.decideEdit(name, recurrenceDay, investments);
        eventStore.append(planId, List.of(event));
    }
}
