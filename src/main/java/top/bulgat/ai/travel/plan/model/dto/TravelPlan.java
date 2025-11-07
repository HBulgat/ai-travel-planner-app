package top.bulgat.ai.travel.plan.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlan implements Serializable {
    private String id;
    private Long userId;
    private String planName;
    private String destination;

    private LocalDate startDate;

    private LocalDate endDate;

    private Double budget;
    private Integer travelers;
    private List<String> activities;
    private List<String> notes;
    private BudgetAnalysis budgetAnalysis;
}

