package com.bux.investmentplans.eventstore;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface EventRowRepository extends JpaRepository<EventRow, java.util.UUID> {

    List<EventRow> findByAggregateIdOrderBySequenceAsc(String aggregateId);

    long countByAggregateId(String aggregateId);
}
