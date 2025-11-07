package top.bulgat.ai.travel.plan.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ExpenseDTO {
    private String id;
    private String travelPlanId;
    private String category;
    private String description;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime expenseDate;
    private String userId;
}