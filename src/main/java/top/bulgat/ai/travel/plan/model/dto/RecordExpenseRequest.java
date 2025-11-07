package top.bulgat.ai.travel.plan.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RecordExpenseRequest {
    private Long travelPlanId;
    private String category;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private String description;
}