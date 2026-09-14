package com.bux.investmentplans.api;

import com.bux.investmentplans.api.generated.PlansApi;
import com.bux.investmentplans.api.generated.model.CreatePlanRequest;
import com.bux.investmentplans.api.generated.model.PlanCreatedResponse;
import com.bux.investmentplans.api.generated.model.PlanDetail;
import com.bux.investmentplans.api.generated.model.PlanSummary;
import com.bux.investmentplans.readmodel.PlanReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Implements the generated {@link PlansApi} (built from {@code
 * investment-plans-api.yaml}). Request/response mapping annotations live on
 * the generated interface, not repeated here — Spring honors them on the
 * implementing bean.
 *
 * <p>Reads (listPlans/getPlan) go straight to the read model (1.3-1.4), no
 * command handler involved. Writes (createPlan/editPlan) go through {@link
 * PlanCommandHandler}.
 */
@RestController
@RequiredArgsConstructor
class PlanController implements PlansApi {

    private final PlanCommandHandler commandHandler;
    private final PlanReadRepository planReads;
    private final PlanApiMapper mapper;

    @Override
    public ResponseEntity<PlanCreatedResponse> createPlan(CreatePlanRequest request) {
        String planId = commandHandler.createPlan(
                request.getName(), request.getRecurrenceDay(), mapper.toDomainInvestments(request.getInvestments()));
        return ResponseEntity.created(URI.create("/plans/" + planId))
                .body(new PlanCreatedResponse().planId(planId));
    }

    @Override
    public ResponseEntity<Void> editPlan(String planId, CreatePlanRequest request) {
        commandHandler.editPlan(
                planId, request.getName(), request.getRecurrenceDay(), mapper.toDomainInvestments(request.getInvestments()));
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<List<PlanSummary>> listPlans() {
        List<PlanSummary> plans = planReads.findAll().stream().map(mapper::toSummary).toList();
        return ResponseEntity.ok(plans);
    }

    @Override
    public ResponseEntity<PlanDetail> getPlan(String planId) {
        return planReads.findById(planId)
                .map(mapper::toDetail)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
