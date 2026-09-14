package com.bux.investmentplans.eventstore;

import org.springframework.data.jpa.repository.JpaRepository;

interface ExecutionIdempotencyRepository extends JpaRepository<ExecutionIdempotencyRow, java.util.UUID> {
}
