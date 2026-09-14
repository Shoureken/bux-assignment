package com.bux.investmentplans.eventstore;

import org.springframework.data.jpa.repository.JpaRepository;

interface OutboxRepository extends JpaRepository<OutboxRow, java.util.UUID> {
}
