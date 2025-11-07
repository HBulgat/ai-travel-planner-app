package top.bulgat.ai.travel.plan.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetAnalysis {
    private Transportation transportation;
    private Accommodation accommodation;
    private Meals meals;
    private Attractions attractions;
    private ShoppingAndMiscellaneous shoppingAndMiscellaneous;
    private Double totalEstimatedExpenses;
    private Double remainingBudget;
    private String budgetStatus;
    private String summary;
    private List<String> recommendedAdjustments;
}
