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
public class Attractions {
    private String description;
    private Double estimatedCost;
    private List<AttractionDetail> details;
    private Double totalEntranceFees;
    private Double bufferForOptionalSites;
    private List<String> costSavingTips;
}
