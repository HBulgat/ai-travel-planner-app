package top.bulgat.ai.travel.plan.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ExpenseResponse {
    private String id;
    private String travelPlanId;
    private String category;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private String description;
}