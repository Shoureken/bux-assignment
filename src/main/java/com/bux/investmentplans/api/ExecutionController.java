package com.bux.investmentplans.api;

import com.bux.investmentplans.api.generated.ExecutionsApi;
import com.bux.investmentplans.api.generated.model.ExecutionDetail;
import com.bux.investmentplans.api.generated.model.ExecutionSummary;
import com.bux.investmentplans.readmodel.ExecutionReadRepository;
import com.bux.investmentplans.readmodel.OrderReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Implements the generated {@link ExecutionsApi}. Pure read side (1.4) —
 * queries {@code ExecutionReadRepository}/{@code OrderReadRepository}
 * directly, same as {@link PlanController}'s GET methods.
 */
@RestController
@RequiredArgsConstructor
class ExecutionController implements ExecutionsApi {

    private final ExecutionReadRepository executionReads;
    private final OrderReadRepository orderReads;
    private final ExecutionApiMapper mapper;

    @Override
    public ResponseEntity<List<ExecutionSummary>> listExecutions(String planId) {
        // Illustrative only: a real query filters by planId with its own
        // repository method (e.g. findByPlanId) rather than findAll().
        List<ExecutionSummary> executions = executionReads.findAll().stream()
                .filter(execution -> execution.getPlanId().equals(planId))
                .map(mapper::toSummary)
                .toList();
        return ResponseEntity.ok(executions);
    }

    @Override
    public ResponseEntity<ExecutionDetail> getExecution(String planId, String executionId) {
        return executionReads.findById(executionId)
                .map(execution -> mapper.toDetail(execution, orderReads.findByIdExecutionId(executionId)))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
