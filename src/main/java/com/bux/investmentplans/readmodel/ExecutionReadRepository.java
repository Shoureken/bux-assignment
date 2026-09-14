package com.bux.investmentplans.readmodel;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionReadRepository extends JpaRepository<ExecutionProjection, String> {
}
