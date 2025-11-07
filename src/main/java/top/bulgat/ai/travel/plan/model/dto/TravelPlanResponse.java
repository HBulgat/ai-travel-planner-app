package top.bulgat.ai.travel.plan.model.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;

@Data
public class TravelPlanResponse {
    private String id;
    private Long userId;
    private String planName;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal budget;
    private Integer travelers;
    private List<String> activities;
    private String notes;
}