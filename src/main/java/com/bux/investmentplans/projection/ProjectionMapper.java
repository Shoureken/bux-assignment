package com.bux.investmentplans.projection;

import com.bux.investmentplans.domain.Investment;
import com.bux.investmentplans.readmodel.InvestmentProjection;
import org.mapstruct.Mapper;

import java.util.List;

/** Domain → read-model mapping for the Projection Worker's write side. */
@Mapper(componentModel = "spring")
interface ProjectionMapper {

    List<InvestmentProjection> toProjections(List<Investment> investments);
}
