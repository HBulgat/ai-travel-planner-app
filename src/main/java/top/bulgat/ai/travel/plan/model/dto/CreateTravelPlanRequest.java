package top.bulgat.ai.travel.plan.model.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;

@Data
public class CreateTravelPlanRequest {
    private Long userId;
    private String planName;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal budget;
    private Integer travelers;
    private String preferences;
    private List<String> details;
    private String notes;
}