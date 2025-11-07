package top.bulgat.ai.travel.plan.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class ExpenseSummaryResponse {
    private BigDecimal totalAmount;
    private Map<String, BigDecimal> amountByCategory;
    private Map<String, BigDecimal> amountByTravelPlan;
}