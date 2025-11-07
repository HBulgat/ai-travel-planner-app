package top.bulgat.ai.travel.plan.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transportation {
    private String description;
    private Double estimatedCost;
    private Map<String, Double> details;
    private List<String> costSavingTips;
}
