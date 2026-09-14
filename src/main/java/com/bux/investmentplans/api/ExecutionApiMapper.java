package com.bux.investmentplans.api;

import com.bux.investmentplans.api.generated.model.ExecutionDetail;
import com.bux.investmentplans.api.generated.model.ExecutionSummary;
import com.bux.investmentplans.api.generated.model.OrderSummary;
import com.bux.investmentplans.readmodel.ExecutionProjection;
import com.bux.investmentplans.readmodel.OrderProjection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
interface ExecutionApiMapper {

    /** {@code status} (String) -> generated {@code ExecutionStatus} via MapStruct's implicit String-to-enum mapping. */
    ExecutionSummary toSummary(ExecutionProjection execution);

    /** {@code orders} is a separate query (readmodel.OrderReadRepository), so it's a second mapper argument, not a nested property. */
    @Mapping(target = "orders", source = "orders")
    ExecutionDetail toDetail(ExecutionProjection execution, List<OrderProjection> orders);

    /** {@code assetId} lives at {@code id.assetId} on the read-model side (2.9) but flat on the API side. */
    @Mapping(target = "assetId", source = "id.assetId")
    OrderSummary toOrderSummary(OrderProjection order);
}
