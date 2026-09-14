package com.bux.investmentplans.readmodel;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderReadRepository extends JpaRepository<OrderProjection, OrderProjectionId> {

    List<OrderProjection> findByIdExecutionId(String executionId);
}
