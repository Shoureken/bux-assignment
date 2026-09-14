package com.bux.investmentplans.api;

import com.bux.investmentplans.api.generated.model.PlanDetail;
import com.bux.investmentplans.api.generated.model.PlanSummary;
import com.bux.investmentplans.domain.Investment;
import com.bux.investmentplans.readmodel.PlanProjection;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * Translation between the OpenAPI-generated layer (models under {@code
 * api.generated}, built from {@code investment-plans-api.yaml}) and this
 * module's own domain/read-model types. Keeps {@link PlanCommandHandler}
 * and the read model free of any dependency on the generated classes.
 * {@code Investment} is a top-level schema in the spec, so it generates as
 * its own class, not a nested one — both {@code CreatePlanRequest} and
 * {@code PlanDetail} reference the same generated {@code Investment} type.
 */
@Mapper(componentModel = "spring")
interface PlanApiMapper {

    List<Investment> toDomainInvestments(List<com.bux.investmentplans.api.generated.model.Investment> investments);

    /** {@code status} (String) -> the generated {@code PlanStatus} enum via MapStruct's implicit String-to-enum mapping. */
    PlanSummary toSummary(PlanProjection projection);

    /** Also maps {@code investments} (embedded {@code InvestmentProjection} list -> generated {@code Investment} list). */
    PlanDetail toDetail(PlanProjection projection);
}
